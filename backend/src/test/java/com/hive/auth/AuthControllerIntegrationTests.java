package com.hive.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerLoginAndRefreshRotateCredentials() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"Alice@example.com","password":"strong-pass-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andReturn();

        JsonNode registered = objectMapper.readTree(registration.getResponse().getContentAsString());
        String firstRefreshToken = registered.get("refreshToken").asText();
        assertThat(registered.get("accessToken").asText()).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"ALICE@EXAMPLE.COM","password":"strong-pass-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRequest(firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedToken = objectMapper.readTree(refresh.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(rotatedToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRequest(firstRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicatesInvalidRequestsAndUnauthenticatedAccess() throws Exception {
        String registration = """
                {"username":"bob","email":"bob@example.com","password":"strong-pass-123"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    private record TokenRequest(String refreshToken) {
    }
}
