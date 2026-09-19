package com.hive.code;

import com.hive.code.dto.CodeSnapshotResponse;
import com.hive.code.dto.SaveCodeSnapshotRequest;
import com.hive.common.InvalidRequestException;
import com.hive.room.RoomMember;
import com.hive.room.RoomPermissionService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeSnapshotService {

    private static final int MAX_CONTENT_LENGTH = 1_000_000;

    private final CodeSnapshotRepository snapshotRepository;
    private final RoomPermissionService permissionService;
    private final Clock clock;

    public CodeSnapshotService(CodeSnapshotRepository snapshotRepository,
                               RoomPermissionService permissionService,
                               Clock clock) {
        this.snapshotRepository = snapshotRepository;
        this.permissionService = permissionService;
        this.clock = clock;
    }

    /** Any room member — including a VIEWER — may load the latest snapshot. */
    @Transactional(readOnly = true)
    public CodeSnapshotResponse latest(Long roomId, Long userId) {
        permissionService.requireMember(roomId, userId);
        return snapshotRepository.findFirstByRoomIdOrderByVersionDesc(roomId)
                .map(CodeSnapshotResponse::from)
                .orElseGet(() -> CodeSnapshotResponse.empty(roomId));
    }

    /** Only OWNER and EDITOR may persist a new version. */
    @Transactional
    public CodeSnapshotResponse save(Long roomId, Long userId, SaveCodeSnapshotRequest request) {
        RoomMember member = permissionService.requireEditor(roomId, userId);
        String content = request == null ? null : request.content();
        if (content == null) {
            throw new InvalidRequestException("Snapshot content is required");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidRequestException("Snapshot content exceeds the maximum supported size");
        }

        int nextVersion = snapshotRepository.findMaxVersion(roomId) + 1;
        // Autosave fires on a timer, so skip writing a new version when nothing actually changed.
        var latest = snapshotRepository.findFirstByRoomIdOrderByVersionDesc(roomId);
        if (latest.isPresent() && latest.get().getContent().equals(content)) {
            return CodeSnapshotResponse.from(latest.get());
        }

        try {
            CodeSnapshot snapshot = snapshotRepository.saveAndFlush(new CodeSnapshot(
                    member.getRoom(),
                    content,
                    nextVersion,
                    member.getUser(),
                    clock.instant().truncatedTo(ChronoUnit.MICROS)));
            return CodeSnapshotResponse.from(snapshot);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent save claimed this version number; the caller can retry with fresh state.
            throw new InvalidRequestException("A newer snapshot was saved concurrently; reload and try again");
        }
    }
}
