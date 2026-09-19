package com.hive.room;

import com.hive.common.ConflictException;
import com.hive.common.ForbiddenException;
import com.hive.common.InvalidRequestException;
import com.hive.common.ResourceNotFoundException;
import com.hive.room.dto.CreateRoomRequest;
import com.hive.room.dto.RoomMemberResponse;
import com.hive.room.dto.RoomResponse;
import com.hive.user.User;
import com.hive.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int INVITE_LENGTH = 10;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RoomPermissionService permissionService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository,
                       UserRepository userRepository, RoomPermissionService permissionService, Clock clock) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.clock = clock;
    }

    @Transactional
    public RoomResponse create(Long userId, CreateRoomRequest request) {
        User owner = findUser(userId);
        Room room = roomRepository.save(new Room(
                request.name().trim(), owner, request.language(), nextInviteCode(), clock.instant()));
        roomMemberRepository.save(new RoomMember(room, owner, RoomRole.OWNER, clock.instant()));
        return RoomResponse.from(room, RoomRole.OWNER, true);
    }

    @Transactional
    public RoomResponse join(Long userId, String inviteCode) {
        String normalizedCode = inviteCode.trim().toUpperCase(Locale.ROOT);
        Room room = roomRepository.findByInviteCodeIgnoreCaseAndActiveTrue(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active room not found for invite code"));
        RoomMember existing = roomMemberRepository.findByRoomIdAndUserId(room.getId(), userId).orElse(null);
        if (existing != null) {
            return RoomResponse.from(room, existing.getRole(), existing.getRole() == RoomRole.OWNER);
        }

        User user = findUser(userId);
        try {
            roomMemberRepository.saveAndFlush(new RoomMember(room, user, RoomRole.EDITOR, clock.instant()));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("User is already a room member");
        }
        return RoomResponse.from(room, RoomRole.EDITOR, false);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listMine(Long userId) {
        return roomMemberRepository.findAllByUserIdAndRoomActiveTrueOrderByRoomCreatedAtDesc(userId)
                .stream()
                .map(member -> RoomResponse.from(
                        member.getRoom(), member.getRole(), member.getRole() == RoomRole.OWNER))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long userId, Long roomId) {
        RoomMember member = permissionService.requireMember(roomId, userId);
        return RoomResponse.from(member.getRoom(), member.getRole(), member.getRole() == RoomRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<RoomMemberResponse> listMembers(Long userId, Long roomId) {
        permissionService.requireMember(roomId, userId);
        return roomMemberRepository.findAllByRoomIdOrderByJoinedAtAsc(roomId)
                .stream()
                .map(RoomMemberResponse::from)
                .toList();
    }

    @Transactional
    public RoomMemberResponse updateMemberRole(Long ownerId, Long roomId, Long memberUserId, RoomRole role) {
        permissionService.requireOwner(roomId, ownerId);
        if (role == RoomRole.OWNER) {
            throw new InvalidRequestException("Ownership transfer is not supported");
        }
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Room member not found"));
        if (member.getRole() == RoomRole.OWNER) {
            throw new InvalidRequestException("The owner role cannot be changed");
        }
        member.changeRole(role);
        return RoomMemberResponse.from(member);
    }

    @Transactional
    public void removeMember(Long ownerId, Long roomId, Long memberUserId) {
        permissionService.requireOwner(roomId, ownerId);
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Room member not found"));
        if (member.getRole() == RoomRole.OWNER) {
            throw new InvalidRequestException("The room owner cannot be removed");
        }
        roomMemberRepository.delete(member);
    }

    @Transactional
    public void leave(Long userId, Long roomId) {
        RoomMember member = permissionService.requireMember(roomId, userId);
        if (member.getRole() == RoomRole.OWNER) {
            throw new InvalidRequestException("The owner must delete the room instead of leaving");
        }
        roomMemberRepository.delete(member);
    }

    @Transactional
    public void delete(Long ownerId, Long roomId) {
        RoomMember owner = permissionService.requireOwner(roomId, ownerId);
        owner.getRoom().deactivate();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String nextInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder value = new StringBuilder(INVITE_LENGTH);
            for (int index = 0; index < INVITE_LENGTH; index++) {
                value.append(INVITE_ALPHABET[secureRandom.nextInt(INVITE_ALPHABET.length)]);
            }
            String code = value.toString();
            if (!roomRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique invite code");
    }
}
