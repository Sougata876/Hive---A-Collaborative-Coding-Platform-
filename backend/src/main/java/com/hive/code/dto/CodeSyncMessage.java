package com.hive.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A CRDT sync handshake or awareness heartbeat carried alongside the update relay.
 *
 * <p>The server never decodes {@code update}: it only authenticates the sender, checks room
 * membership, and rebroadcasts. {@code req} asks peers for their resolved document state, {@code res}
 * answers one specific peer, and {@code awareness} carries a JSON cursor position (see
 * {@code frontend/src/lib/yjsProvider.js}). Without the request/response pair a client that joins
 * after the first editor would have no way to learn the document's current content, since the server
 * keeps only text snapshots and seeding text locally would fork the CRDT history and duplicate it on
 * merge.
 */
public record CodeSyncMessage(
        @NotBlank
        @Pattern(regexp = "req|res|awareness", message = "must be req, res or awareness")
        String kind,

        Long target,

        @Size(max = 262_144)
        String update
) {
}
