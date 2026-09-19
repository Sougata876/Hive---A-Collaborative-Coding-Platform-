CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender
        FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_chat_messages_room_sent_id (room_id, sent_at, id),
    INDEX idx_chat_messages_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
