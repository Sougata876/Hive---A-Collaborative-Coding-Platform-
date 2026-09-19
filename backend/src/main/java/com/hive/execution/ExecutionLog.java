package com.hive.execution;

import com.hive.room.Room;
import com.hive.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "execution_logs",
        indexes = @Index(name = "idx_execution_logs_room_executed_at", columnList = "room_id, executed_at"))
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by", nullable = false, updatable = false)
    private User triggeredBy;

    @Lob
    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String code;

    @Lob
    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String output;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ExecutionStatus status;

    @Column(name = "exit_code", updatable = false)
    private Integer exitCode;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    protected ExecutionLog() {
    }

    public ExecutionLog(Room room, User triggeredBy, String code, ExecutionResult result, Instant executedAt) {
        this.room = Objects.requireNonNull(room);
        this.triggeredBy = Objects.requireNonNull(triggeredBy);
        this.code = Objects.requireNonNull(code);
        this.status = result.status();
        this.output = buildOutput(result);
        this.exitCode = result.exitCode();
        this.durationMs = result.durationMs();
        this.executedAt = Objects.requireNonNull(executedAt);
    }

    private static String buildOutput(ExecutionResult result) {
        if (result.stderr() == null || result.stderr().isBlank()) {
            return result.stdout();
        }
        return result.stdout() + "\n" + result.stderr();
    }

    public Long getId() {
        return id;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
