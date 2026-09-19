package com.hive.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UserProfileControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void authenticatedUserCanReadAndUpdateProfile() throws Exception {
        String accessToken = register("profile_user", "profile@example.com");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profile_user"))
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"renamed_user","email":"NEW@example.com","avatarUrl":"https://example.com/avatar.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("renamed_user"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("renamed_user"));
    }

    @Test
    void updateRejectsConflictsAndInvalidAvatarUrls() throws Exception {
        register("existing_user", "existing@example.com");
        String accessToken = register("other_user", "other@example.com");

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"EXISTING_USER\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.avatarUrl").exists());
    }

    @Test
    void invalidAccessTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    private String register(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", email,
                                "password", "strong-pass-123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
