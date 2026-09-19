package com.hive.room;

import com.hive.common.ForbiddenException;
import com.hive.common.ResourceNotFoundException;
import java.util.Arrays;
import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomPermissionService {

    private final RoomMemberRepository roomMemberRepository;

    public RoomPermissionService(RoomMemberRepository roomMemberRepository) {
        this.roomMemberRepository = roomMemberRepository;
    }

    @Transactional(readOnly = true)
    public RoomMember requireMember(Long roomId, Long userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this room"));
        if (!member.getRoom().isActive()) {
            throw new ResourceNotFoundException("Room not found");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public RoomMember requireAnyRole(Long roomId, Long userId, RoomRole... roles) {
        RoomMember member = requireMember(roomId, userId);
        EnumSet<RoomRole> allowed = EnumSet.copyOf(Arrays.asList(roles));
        if (!allowed.contains(member.getRole())) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
        return member;
    }

    public RoomMember requireOwner(Long roomId, Long userId) {
        return requireAnyRole(roomId, userId, RoomRole.OWNER);
    }

    public RoomMember requireEditor(Long roomId, Long userId) {
        return requireAnyRole(roomId, userId, RoomRole.OWNER, RoomRole.EDITOR);
    }
}
