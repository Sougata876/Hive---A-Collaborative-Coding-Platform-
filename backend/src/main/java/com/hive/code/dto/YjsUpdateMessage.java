package com.hive.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A base64-encoded Yjs binary update relayed between room members. The server never decodes the
 * CRDT payload; it only authorizes the sender and rebroadcasts to the room.
 */
public record YjsUpdateMessage(
        @NotBlank
        @Size(max = 262_144)
        String update
) {
}
