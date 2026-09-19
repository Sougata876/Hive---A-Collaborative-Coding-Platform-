package com.hive.room;

import com.hive.room.dto.CreateRoomRequest;
import com.hive.room.dto.JoinRoomRequest;
import com.hive.room.dto.RoomMemberResponse;
import com.hive.room.dto.RoomResponse;
import com.hive.room.dto.UpdateMemberRoleRequest;
import com.hive.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
                               @Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(principal.id(), request);
    }

    @PostMapping("/join")
    public RoomResponse join(@AuthenticationPrincipal AuthenticatedUser principal,
                             @Valid @RequestBody JoinRoomRequest request) {
        return roomService.join(principal.id(), request.inviteCode());
    }

    @GetMapping
    public List<RoomResponse> listMine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return roomService.listMine(principal.id());
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(@AuthenticationPrincipal AuthenticatedUser principal,
                            @PathVariable Long roomId) {
        return roomService.get(principal.id(), roomId);
    }

    @GetMapping("/{roomId}/members")
    public List<RoomMemberResponse> listMembers(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long roomId) {
        return roomService.listMembers(principal.id(), roomId);
    }

    @PatchMapping("/{roomId}/members/{memberUserId}/role")
    public RoomMemberResponse updateMemberRole(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long roomId,
                                               @PathVariable Long memberUserId,
                                               @Valid @RequestBody UpdateMemberRoleRequest request) {
        return roomService.updateMemberRole(principal.id(), roomId, memberUserId, request.role());
    }

    @DeleteMapping("/{roomId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AuthenticatedUser principal,
                             @PathVariable Long roomId,
                             @PathVariable Long memberUserId) {
        roomService.removeMember(principal.id(), roomId, memberUserId);
    }

    @DeleteMapping("/{roomId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long roomId) {
        roomService.leave(principal.id(), roomId);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long roomId) {
        roomService.delete(principal.id(), roomId);
    }
}
