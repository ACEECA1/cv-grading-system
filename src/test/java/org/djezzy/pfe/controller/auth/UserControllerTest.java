package org.djezzy.pfe.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dto.auth.UpdateUserRequest;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.service.auth.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    @WithMockUser(username = "testuser")
    void testGetCurrentUser() throws Exception {
        UserDTO user = new UserDTO(1L, "testuser", "John", "Doe", "john@test.com", null, true, null, null, null);
        
        Mockito.when(userService.resolveUserIdByPrincipal("testuser")).thenReturn(1L);
        Mockito.when(userService.getUserProfile(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user fetched successfully"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateCurrentUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("Johnny", "Doe", "johnny@test.com", "newpassword123");
        UserDTO updatedUser = new UserDTO(1L, "testuser", "Johnny", "Doe", "johnny@test.com", null, true, null, null, null);
        
        Mockito.when(userService.resolveUserIdByPrincipal("testuser")).thenReturn(1L);
        Mockito.when(userService.updateUserProfile(eq(1L), any(UpdateUserRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Johnny"));
    }
}
