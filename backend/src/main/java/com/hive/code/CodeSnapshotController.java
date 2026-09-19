package com.hive.code;

import com.hive.code.dto.CodeSnapshotResponse;
import com.hive.code.dto.SaveCodeSnapshotRequest;
import com.hive.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/snapshots")
public class CodeSnapshotController {

    private final CodeSnapshotService snapshotService;

    public CodeSnapshotController(CodeSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/latest")
    public CodeSnapshotResponse latest(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long roomId) {
        return snapshotService.latest(roomId, principal.id());
    }

    @PostMapping
    public CodeSnapshotResponse save(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long roomId,
                                     @Valid @RequestBody SaveCodeSnapshotRequest request) {
        return snapshotService.save(roomId, principal.id(), request);
    }
}
