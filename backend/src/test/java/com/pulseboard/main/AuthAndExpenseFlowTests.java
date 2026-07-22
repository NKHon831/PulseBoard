package com.pulseboard.main;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndExpenseFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupLoginAndManageExpenses() throws Exception {
        String email = "jane.doe+" + System.nanoTime() + "@example.com";

        // Unauthenticated requests to protected routes are rejected.
        mockMvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());

        // Signup issues a token immediately.
        String signupBody = objectMapper.writeValueAsString(
                Map.of("name", "Jane Doe", "email", email, "password", "supersecret1"));
        String signupResponse = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String signupToken = JsonPath.read(signupResponse, "$.token");

        // Signing up twice with the same email is rejected.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isConflict());

        // The issued token authenticates /api/auth/me.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + signupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // Login with correct credentials succeeds; wrong password is rejected.
        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret1"));
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        // Creating an expense persists it, scoped to this user.
        String expenseBody = objectMapper.writeValueAsString(Map.of(
                "amount", 42.50,
                "category", "Lunch",
                "description", "Lunch",
                "expenseDate", LocalDate.now().toString()));
        String createResponse = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(42.50))
                .andExpect(jsonPath("$.category").value("Lunch"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String expenseId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expenseId));

        // Deleting removes it, and it no longer shows up in the list.
        mockMvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Deleting again (already gone) is a 404, not silently ignored.
        mockMvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
