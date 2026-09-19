package com.hive.code;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CodeSnapshotControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void snapshotsAreVersionedAndLatestIsReturned() throws Exception {
        Session owner = register("snap_owner", "snap-owner@example.com");
        JsonNode room = createRoom(owner.token(), "Snapshot room");
        long roomId = room.get("id").asLong();

        mockMvc.perform(get("/api/rooms/{roomId}/snapshots/latest", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.content").value(""));

        saveSnapshot(owner.token(), roomId, "class Main {}", 1);
        saveSnapshot(owner.token(), roomId, "class Main { void run() {} }", 2);

        mockMvc.perform(get("/api/rooms/{roomId}/snapshots/latest", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.content").value("class Main { void run() {} }"))
                .andExpect(jsonPath("$.savedByUsername").value("snap_owner"));
    }

    @Test
    void viewersCanLoadButNotSaveAndOutsidersAreRejected() throws Exception {
        Session owner = register("view_owner", "view-owner@example.com");
        Session viewer = register("view_member", "view-member@example.com");
        Session outsider = register("view_outsider", "view-outsider@example.com");
        JsonNode room = createRoom(owner.token(), "Viewer room");
        long roomId = room.get("id").asLong();
        joinRoom(viewer.token(), room.get("inviteCode").asText());

        mockMvc.perform(patch("/api/rooms/{roomId}/members/{userId}/role", roomId, viewer.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk());
        saveSnapshot(owner.token(), roomId, "class Main {}", 1);

        mockMvc.perform(get("/api/rooms/{roomId}/snapshots/latest", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("class Main {}"));

        mockMvc.perform(post("/api/rooms/{roomId}/snapshots", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "hacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/rooms/{roomId}/snapshots/latest", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedIdenticalContentDoesNotCreateNewVersions() throws Exception {
        Session owner = register("noop_owner", "noop-owner@example.com");
        JsonNode room = createRoom(owner.token(), "Autosave room");
        long roomId = room.get("id").asLong();

        saveSnapshot(owner.token(), roomId, "class Main {}", 1);
        // An autosave firing with unchanged content should return the existing version, not bump it.
        saveSnapshot(owner.token(), roomId, "class Main {}", 1);
        saveSnapshot(owner.token(), roomId, "class Main { int x; }", 2);

        mockMvc.perform(get("/api/rooms/{roomId}/snapshots/latest", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    private void saveSnapshot(String token, long roomId, String content, int expectedVersion) throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/snapshots", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(expectedVersion));
    }

    private JsonNode createRoom(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "language", "JAVA"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void joinRoom(String token, String inviteCode) throws Exception {
        mockMvc.perform(post("/api/rooms/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inviteCode", inviteCode))))
                .andExpect(status().isOk());
    }

    private Session register(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "email", email, "password", "strong-pass-123"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Session(response.get("accessToken").asText(), response.get("user").get("id").asLong());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(String token, long userId) {
    }
}
