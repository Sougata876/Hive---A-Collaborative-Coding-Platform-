package com.hive.execution;

import com.hive.common.InvalidRequestException;
import com.hive.execution.dto.ExecuteCodeRequest;
import com.hive.execution.dto.ExecutionResponse;
import com.hive.room.ProgrammingLanguage;
import com.hive.room.Room;
import com.hive.room.RoomMember;
import com.hive.room.RoomPermissionService;
import com.hive.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates sandboxed code execution: authorizes the caller, enforces a per-room rate limit, caps
 * how many containers run at once, and records the outcome.
 */
@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);

    private final List<LanguageExecutor> languageExecutors;
    private final RoomPermissionService permissionService;
    private final ExecutionLogRepository logRepository;
    private final ExecutionProperties properties;
    private final Clock clock;
    private final Semaphore executionSlots;

    public CodeExecutionService(List<LanguageExecutor> languageExecutors,
                               RoomPermissionService permissionService,
                               ExecutionLogRepository logRepository,
                               ExecutionProperties properties,
                               Clock clock) {
        this.languageExecutors = List.copyOf(languageExecutors);
        this.permissionService = permissionService;
        this.logRepository = logRepository;
        this.properties = properties;
        this.clock = clock;
        // Bound concurrent containers so one busy room cannot exhaust the host.
        this.executionSlots = new Semaphore(properties.maxConcurrentExecutions(), true);
    }

    /** Resolves the executor for a language, or empty when none is registered. */
    private Optional<LanguageExecutor> executorFor(ProgrammingLanguage language) {
        return languageExecutors.stream()
                .filter(executor -> language == executor.language())
                .findFirst();
    }

    public ExecutionResponse execute(Long roomId, Long userId, ExecuteCodeRequest request) {
        RoomMember member = permissionService.requireEditor(roomId, userId);
        Room room = member.getRoom();
        User user = member.getUser();

        String code = request == null ? null : request.code();
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Code is required");
        }
        if (code.length() > properties.maxSourceLength()) {
            throw new InvalidRequestException("Code exceeds the maximum supported size");
        }

        LanguageExecutor executor = executorFor(room.getLanguage())
                .orElseThrow(() -> new InvalidRequestException(
                        "Execution is not supported for " + room.getLanguage() + " yet"));

        enforceRateLimit(roomId);

        ExecutionResult result = runSerialized(executor, code);
        recordExecution(room, user, code, result);
        return ExecutionResponse.from(result);
    }

    private void enforceRateLimit(Long roomId) {
        Instant windowStart = clock.instant().minus(Duration.ofMinutes(1));
        long recent = logRepository.countByRoomIdAndExecutedAtAfter(roomId, windowStart);
        if (recent >= properties.perRoomRateLimitPerMinute()) {
            throw new RateLimitExceededException(
                    "This room has reached its execution limit. Try again in a moment.");
        }
    }

    /** Queues the run so a room cannot flood the host with concurrent containers. */
    private ExecutionResult runSerialized(LanguageExecutor executor, String code) {
        boolean acquired = false;
        try {
            // Wait a bounded time for a slot, then shed load rather than queueing without limit.
            acquired = executionSlots.tryAcquire(
                    properties.timeout().toMillis() * 2, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new RateLimitExceededException(
                        "The execution queue is busy. Try again in a moment.");
            }
            return executor.execute(code);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ExecutionResult.error("Execution was interrupted.");
        } finally {
            if (acquired) {
                executionSlots.release();
            }
        }
    }

    /**
     * Persists the attempt in its own transaction. A logging failure must not discard a result the
     * user already waited for.
     */
    @Transactional
    void recordExecution(Room room, User user, String code, ExecutionResult result) {
        try {
            logRepository.save(new ExecutionLog(
                    room, user, code, result, clock.instant().truncatedTo(ChronoUnit.MICROS)));
        } catch (RuntimeException exception) {
            log.warn("Could not record the execution log for room {}", room.getId(), exception);
        }
    }
}
