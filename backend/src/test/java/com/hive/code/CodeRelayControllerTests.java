package com.hive.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hive.code.dto.CodeSyncMessage;
import com.hive.code.dto.YjsUpdateBroadcast;
import com.hive.code.dto.YjsUpdateMessage;
import com.hive.common.ForbiddenException;
import com.hive.room.Room;
import com.hive.room.RoomMember;
import com.hive.room.RoomPermissionService;
import com.hive.room.RoomRole;
import com.hive.security.AuthenticatedUser;
import com.hive.user.User;
import com.hive.websocket.WebSocketAccessDeniedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class CodeRelayControllerTests {

    RoomPermissionService permissionService;
    SimpMessagingTemplate messagingTemplate;
    CodeRelayController controller;
    AuthenticatedUser editor;

    @BeforeEach
    void setUp() {
        permissionService = mock(RoomPermissionService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneOffset.UTC);
        controller = new CodeRelayController(permissionService, messagingTemplate, clock);
        editor = new AuthenticatedUser(3L, "alice", "hash");
    }

    @Test
    void relaysUpdateToRoomCodeTopicWithAuthenticatedOrigin() {
        when(permissionService.requireEditor(9L, 3L)).thenReturn(member("alice"));

        controller.relay(9L, new YjsUpdateMessage("AQIDBA=="), principal(editor));

        YjsUpdateBroadcast broadcast = captureBroadcast();
        assertThat(broadcast.userId()).isEqualTo(3L);
        assertThat(broadcast.username()).isEqualTo("alice");
        assertThat(broadcast.update()).isEqualTo("AQIDBA==");
        assertThat(broadcast.roomId()).isEqualTo(9L);
        assertThat(broadcast.kind()).isEqualTo("update");
    }

    @Test
    void viewersCannotRelayUpdates() {
        when(permissionService.requireEditor(9L, 3L))
                .thenThrow(new ForbiddenException("You do not have permission to perform this action"));

        assertThatThrownBy(() -> controller.relay(9L, new YjsUpdateMessage("AQID"), principal(editor)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void unauthenticatedPrincipalIsRejectedBeforeAnyPermissionLookup() {
        assertThatThrownBy(() -> controller.relay(9L, new YjsUpdateMessage("AQID"), () -> "anonymous"))
                .isInstanceOf(WebSocketAccessDeniedException.class);
        verifyNoInteractions(permissionService, messagingTemplate);
    }

    @Test
    void relaysSyncRequestToTheRoomSoPeersCanCatchUp() {
        when(permissionService.requireMember(9L, 3L)).thenReturn(member("carol"));

        controller.sync(9L, new CodeSyncMessage("req", null, "BgcICQ=="), principal(editor));

        YjsUpdateBroadcast broadcast = captureBroadcast();
        assertThat(broadcast.kind()).isEqualTo("req");
        assertThat(broadcast.target()).isNull();
        assertThat(broadcast.userId()).isEqualTo(3L);
        assertThat(broadcast.username()).isEqualTo("carol");
        assertThat(broadcast.update()).isEqualTo("BgcICQ==");
    }

    @Test
    void relaysSyncResponseToItsTargetPeer() {
        when(permissionService.requireMember(9L, 3L)).thenReturn(member("alice"));

        controller.sync(9L, new CodeSyncMessage("res", 42L, "BAoLDA=="), principal(editor));

        YjsUpdateBroadcast broadcast = captureBroadcast();
        assertThat(broadcast.kind()).isEqualTo("res");
        assertThat(broadcast.target()).isEqualTo(42L);
    }

    @Test
    void nonMembersCannotStartASyncHandshake() {
        when(permissionService.requireMember(9L, 3L))
                .thenThrow(new ForbiddenException("You are not a member of this room"));

        assertThatThrownBy(() ->
                controller.sync(9L, new CodeSyncMessage("req", null, "AA=="), principal(editor)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messagingTemplate);
    }

    private YjsUpdateBroadcast captureBroadcast() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/room/9/code"), payload.capture());
        return (YjsUpdateBroadcast) payload.getValue();
    }

    private UsernamePasswordAuthenticationToken principal(AuthenticatedUser user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private RoomMember member(String username) {
        Instant now = Instant.parse("2026-09-13T09:00:00Z");
        User user = new User(username, username + "@example.com", "hash", now);
        Room room = new Room("Room", user, com.hive.room.ProgrammingLanguage.JAVA, "INVITE1234", now);
        return new RoomMember(room, user, RoomRole.EDITOR, now);
    }
}
