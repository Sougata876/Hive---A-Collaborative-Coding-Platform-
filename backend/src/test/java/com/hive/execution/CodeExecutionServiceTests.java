package com.hive.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hive.common.ForbiddenException;
import com.hive.common.InvalidRequestException;
import com.hive.execution.dto.ExecuteCodeRequest;
import com.hive.room.ProgrammingLanguage;
import com.hive.room.Room;
import com.hive.room.RoomMember;
import com.hive.room.RoomPermissionService;
import com.hive.room.RoomRole;
import com.hive.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodeExecutionServiceTests {

    RoomPermissionService permissionService;
    ExecutionLogRepository logRepository;
    LanguageExecutor javaExecutor;
    CodeExecutionService service;

    @BeforeEach
    void setUp() {
        permissionService = mock(RoomPermissionService.class);
        logRepository = mock(ExecutionLogRepository.class);
        javaExecutor = mock(LanguageExecutor.class);
        when(javaExecutor.language()).thenReturn(ProgrammingLanguage.JAVA);

        var properties = new ExecutionProperties(
                "openjdk:21-slim", "docker", Duration.ofSeconds(1), "128m", "0.5", 100, 1000, 1, 3);
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T10:00:00Z"), ZoneOffset.UTC);
        service = new CodeExecutionService(
                List.of(javaExecutor), permissionService, logRepository, properties, clock);
        when(permissionService.requireEditor(5L, 7L)).thenReturn(member());
    }

    @Test
    void runsCodeInTheSandboxAndRecordsTheResult() {
        var result = new ExecutionResult(ExecutionStatus.SUCCESS, "Hello Hive\n", "", 0, 42);
        when(javaExecutor.execute("class Main {}")).thenReturn(result);

        var response = service.execute(5L, 7L, new ExecuteCodeRequest("class Main {}"));

        assertThat(response.status()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(response.stdout()).isEqualTo("Hello Hive\n");
        assertThat(response.exitCode()).isZero();
        verify(logRepository).save(org.mockito.ArgumentMatchers.any(ExecutionLog.class));
    }

    @Test
    void viewersCannotExecuteAndNothingIsRun() {
        when(permissionService.requireEditor(5L, 9L))
                .thenThrow(new ForbiddenException("You do not have permission to perform this action"));

        assertThatThrownBy(() -> service.execute(5L, 9L, new ExecuteCodeRequest("class Main {}")))
                .isInstanceOf(ForbiddenException.class);
        verify(javaExecutor, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rateLimitBlocksAFloodingRoomBeforeStartingAContainer() {
        when(logRepository.countByRoomIdAndExecutedAtAfter(
                org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.execute(5L, 7L, new ExecuteCodeRequest("class Main {}")))
                .isInstanceOf(RateLimitExceededException.class);
        verify(javaExecutor, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsBlankAndOversizedSource() {
        assertThatThrownBy(() -> service.execute(5L, 7L, new ExecuteCodeRequest("   ")))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.execute(5L, 7L, new ExecuteCodeRequest("x".repeat(101))))
                .isInstanceOf(InvalidRequestException.class);
        verify(javaExecutor, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void timeoutAndFailureStatusesReachTheCaller() {
        when(javaExecutor.execute("loop"))
                .thenReturn(ExecutionResult.timedOut("partial", "", Duration.ofSeconds(10)));
        assertThat(service.execute(5L, 7L, new ExecuteCodeRequest("loop")).status())
                .isEqualTo(ExecutionStatus.TIMEOUT);

        when(javaExecutor.execute("bad"))
                .thenReturn(new ExecutionResult(ExecutionStatus.FAILED, "", "error: ';' expected", 1, 12));
        var failed = service.execute(5L, 7L, new ExecuteCodeRequest("bad"));
        assertThat(failed.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(failed.stderr()).contains("';' expected");
    }

    @Test
    void anUnsupportedRoomLanguageIsReportedRatherThanRunAsJava() {
        var service = new CodeExecutionService(
                List.of(), permissionService, logRepository,
                new ExecutionProperties(null, null, null, null, null, 0, 0, 0, 0),
                Clock.fixed(Instant.parse("2026-09-14T10:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.execute(5L, 7L, new ExecuteCodeRequest("class Main {}")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not supported");
    }

    private RoomMember member() {
        Instant now = Instant.parse("2026-09-14T09:00:00Z");
        User user = new User("alice", "alice@example.com", "hash", now);
        Room room = new Room("Room", user, ProgrammingLanguage.JAVA, "INVITE1234", now);
        return new RoomMember(room, user, RoomRole.EDITOR, now);
    }
}
