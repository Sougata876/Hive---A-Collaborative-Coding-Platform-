package com.hive.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(@NotBlank @Size(max = 32) String inviteCode) {
}
