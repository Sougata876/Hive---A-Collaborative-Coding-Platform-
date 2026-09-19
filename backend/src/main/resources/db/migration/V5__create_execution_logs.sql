CREATE TABLE execution_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    triggered_by BIGINT NOT NULL,
    code LONGTEXT NOT NULL,
    output LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    exit_code INT NULL,
    duration_ms BIGINT NOT NULL,
    executed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_execution_logs_room
        FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_execution_logs_triggered_by
        FOREIGN KEY (triggered_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_execution_logs_room_executed_at (room_id, executed_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
