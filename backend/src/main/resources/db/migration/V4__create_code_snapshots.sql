CREATE TABLE code_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    version INT NOT NULL,
    saved_by BIGINT NOT NULL,
    saved_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_code_snapshots_room_version UNIQUE (room_id, version),
    CONSTRAINT fk_code_snapshots_room
        FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_code_snapshots_saved_by
        FOREIGN KEY (saved_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_code_snapshots_room_version (room_id, version)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
