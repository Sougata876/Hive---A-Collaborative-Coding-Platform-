package com.hive.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class RoomControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void ownerCreatesRoomAndUserJoinsWithEditorRole() throws Exception {
        Session owner = register("room_owner", "owner@rooms.test");
        Session editor = register("room_editor", "editor@rooms.test");
        JsonNode room = createRoom(owner.token(), "Pairing room");
        long roomId = room.get("id").asLong();
        String inviteCode = room.get("inviteCode").asText();

        mockMvc.perform(post("/api/rooms/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(editor.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inviteCode", inviteCode.toLowerCase()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andExpect(jsonPath("$.inviteCode").doesNotExist());

        mockMvc.perform(get("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(editor.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roomId))
                .andExpect(jsonPath("$[0].role").value("EDITOR"));

        mockMvc.perform(get("/api/rooms/{roomId}/members", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void ownerManagesRolesAndEditorsCannotUseOwnerActions() throws Exception {
        Session owner = register("role_owner", "role-owner@rooms.test");
        Session member = register("role_member", "role-member@rooms.test");
        JsonNode room = createRoom(owner.token(), "Role room");
        long roomId = room.get("id").asLong();
        joinRoom(member.token(), room.get("inviteCode").asText());

        mockMvc.perform(patch("/api/rooms/{roomId}/members/{userId}/role", roomId, member.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(patch("/api/rooms/{roomId}/members/{userId}/role", roomId, owner.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(member.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"EDITOR\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/rooms/{roomId}/members/{userId}/role", roomId, member.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void membershipAndSoftDeletionAreEnforced() throws Exception {
        Session owner = register("delete_owner", "delete-owner@rooms.test");
        Session member = register("delete_member", "delete-member@rooms.test");
        Session outsider = register("room_outsider", "outsider@rooms.test");
        JsonNode room = createRoom(owner.token(), "Temporary room");
        long roomId = room.get("id").asLong();
        joinRoom(member.token(), room.get("inviteCode").asText());

        mockMvc.perform(get("/api/rooms/{roomId}", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/rooms/{roomId}/members/me", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/rooms/{roomId}/members/{userId}", roomId, member.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/rooms/{roomId}", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/rooms/{roomId}", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private JsonNode createRoom(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "language", "JAVA"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
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
        assertThat(response.get("accessToken").asText()).isNotBlank();
        return new Session(response.get("accessToken").asText(), response.get("user").get("id").asLong());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(String token, long userId) {
    }
}
