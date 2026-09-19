package com.hive.room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    @EntityGraph(attributePaths = {"room", "room.owner"})
    List<RoomMember> findAllByUserIdAndRoomActiveTrueOrderByRoomCreatedAtDesc(Long userId);

    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<RoomMember> findAllByRoomIdOrderByJoinedAtAsc(Long roomId);
}
