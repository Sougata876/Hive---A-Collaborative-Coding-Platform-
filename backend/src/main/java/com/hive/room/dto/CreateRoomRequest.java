package com.hive.room.dto;

import com.hive.room.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ProgrammingLanguage language
) {
}
