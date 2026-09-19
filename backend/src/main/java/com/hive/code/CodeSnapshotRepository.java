package com.hive.code;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, Long> {

    @EntityGraph(attributePaths = "savedBy")
    Optional<CodeSnapshot> findFirstByRoomIdOrderByVersionDesc(Long roomId);

    @Query("SELECT COALESCE(MAX(snapshot.version), 0) FROM CodeSnapshot snapshot WHERE snapshot.room.id = :roomId")
    int findMaxVersion(@Param("roomId") Long roomId);
}
