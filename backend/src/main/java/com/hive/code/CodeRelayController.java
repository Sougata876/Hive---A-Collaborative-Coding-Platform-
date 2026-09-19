package com.hive.code;

import com.hive.code.dto.CodeSyncMessage;
import com.hive.code.dto.YjsUpdateBroadcast;
import com.hive.code.dto.YjsUpdateMessage;
import com.hive.room.RoomPermissionService;
import com.hive.security.AuthenticatedUser;
import com.hive.websocket.WebSocketAccessDeniedException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * Relays opaque Yjs CRDT updates between room members. The document itself is resolved on the
 * clients; the server's job is to authenticate the origin, authorize writes, and rebroadcast.
 */
@Controller
public class CodeRelayController {

    private static final Logger log = LoggerFactory.getLogger(CodeRelayController.class);

    private final RoomPermissionService permissionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public CodeRelayController(RoomPermissionService permissionService,
                               SimpMessagingTemplate messagingTemplate,
                               Clock clock) {
        this.permissionService = permissionService;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @MessageMapping("/room/{roomId}/edit")
    public void relay(@DestinationVariable Long roomId,
                      @Valid @Payload YjsUpdateMessage message,
                      Principal principal) {
        AuthenticatedUser user = authenticatedUser(principal);
        // Re-check the role here: never rely on the interceptor alone for write access.
        var member = permissionService.requireEditor(roomId, user.id());

        YjsUpdateBroadcast broadcast = new YjsUpdateBroadcast(
                roomId,
                user.id(),
                member.getUser().getUsername(),
                message.update(),
                clock.instant());
        broadcast(roomId, broadcast);
    }

    /**
     * Handles a CRDT sync handshake. Both directions only require membership: reading the shared
     * document is what a VIEWER is allowed to do, and answering a peer leaks nothing the peer cannot
     * already see. Replies carry {@code target} so the requester can pick out its own answer.
     */
    @MessageMapping("/room/{roomId}/sync")
    public void sync(@DestinationVariable Long roomId,
                     @Valid @Payload CodeSyncMessage message,
                     Principal principal) {
        AuthenticatedUser user = authenticatedUser(principal);
        var member = permissionService.requireMember(roomId, user.id());
        broadcast(roomId, new YjsUpdateBroadcast(roomId, user.id(), member.getUser().getUsername(),
                message.update(), clock.instant(), message.kind(), message.target()));
    }

    private void broadcast(Long roomId, YjsUpdateBroadcast payload) {
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/code", payload);
        } catch (RuntimeException exception) {
            // Yjs updates are idempotent and clients resync, so a dropped relay is recoverable.
            log.warn("Failed to relay a {} message for room {}", payload.kind(), roomId, exception);
        }
    }

    private AuthenticatedUser authenticatedUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new WebSocketAccessDeniedException("Authentication is required");
    }
}
