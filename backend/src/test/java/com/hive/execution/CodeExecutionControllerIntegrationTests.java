package com.hive.execution;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hive.room.ProgrammingLanguage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the HTTP contract with the sandbox stubbed out, so these run without Docker. The real
 * sandbox is exercised by {@link DockerJavaExecutorIntegrationTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CodeExecutionControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LanguageExecutor languageExecutor;

    @BeforeEach
    void stubExecutor() {
        when(languageExecutor.language()).thenReturn(ProgrammingLanguage.JAVA);
        when(languageExecutor.execute(anyString()))
                .thenReturn(new ExecutionResult(ExecutionStatus.SUCCESS, "Hello Hive!\n", "", 0, 25));
    }

    @Test
    void editorRunsCodeAndReceivesCapturedOutput() throws Exception {
        Session owner = register("exec_owner", "exec-owner@example.com");
        long roomId = createRoom(owner.token(), "Execution room").get("id").asLong();

        mockMvc.perform(post("/api/rooms/{roomId}/executions", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("code", "public class Main { public static void main(String[] a) {} }"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.stdout").value("Hello Hive!\n"))
                .andExpect(jsonPath("$.exitCode").value(0));
    }

    @Test
    void viewersAndOutsidersCannotRunCode() throws Exception {
        Session owner = register("exec_owner2", "exec-owner2@example.com");
        Session viewer = register("exec_viewer", "exec-viewer@example.com");
        Session outsider = register("exec_outsider", "exec-outsider@example.com");
        JsonNode room = createRoom(owner.token(), "Restricted room");
        long roomId = room.get("id").asLong();
        joinRoom(viewer.token(), room.get("inviteCode").asText());

        mockMvc.perform(patch("/api/rooms/{roomId}/members/{userId}/role", roomId, viewer.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/{roomId}/executions", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(viewer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "class Main {}"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/rooms/{roomId}/executions", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "class Main {}"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void blankCodeIsRejected() throws Exception {
        Session owner = register("exec_owner3", "exec-owner3@example.com");
        long roomId = createRoom(owner.token(), "Validation room").get("id").asLong();

        mockMvc.perform(post("/api/rooms/{roomId}/executions", roomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "   "))))
                .andExpect(status().isBadRequest());
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
