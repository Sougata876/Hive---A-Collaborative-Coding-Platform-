package com.hive.room;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByInviteCodeIgnoreCaseAndActiveTrue(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
