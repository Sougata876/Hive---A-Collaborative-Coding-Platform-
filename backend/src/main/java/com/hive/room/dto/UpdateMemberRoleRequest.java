package com.hive.room.dto;

import com.hive.room.RoomRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull RoomRole role) {
}
