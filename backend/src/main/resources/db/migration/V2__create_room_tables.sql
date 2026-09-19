CREATE TABLE rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    language VARCHAR(20) NOT NULL,
    invite_code VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rooms_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_rooms_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE room_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_room_members_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_room_members_user_id (user_id)
) ENGINE=InnoDB;
