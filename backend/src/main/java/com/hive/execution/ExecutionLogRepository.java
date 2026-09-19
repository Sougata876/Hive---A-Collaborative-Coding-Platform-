package com.hive.execution;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    long countByRoomIdAndExecutedAtAfter(Long roomId, Instant threshold);
}
