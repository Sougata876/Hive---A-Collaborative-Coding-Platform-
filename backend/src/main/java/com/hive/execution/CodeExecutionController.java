package com.hive.execution;

import com.hive.execution.dto.ExecuteCodeRequest;
import com.hive.execution.dto.ExecutionResponse;
import com.hive.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/executions")
public class CodeExecutionController {

    private final CodeExecutionService executionService;

    public CodeExecutionController(CodeExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ExecutionResponse execute(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long roomId,
                                     @Valid @RequestBody ExecuteCodeRequest request) {
        return executionService.execute(roomId, principal.id(), request);
    }
}
