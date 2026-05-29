package org.djezzy.pfe.controller.auth;

import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.service.auth.AdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.djezzy.pfe.filter.JwtAuthenticationFilter;
import org.djezzy.pfe.filter.ApiKeyAuthenticationFilter;
import org.djezzy.pfe.service.auth.CustomUserDetailsService;
import org.djezzy.pfe.config.AppProperties;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void testPendingRh() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "rh1", "firstName", "lastName", "rh1@test.com", null, true, null, null, null);
        Mockito.when(adminService.listPendingHr()).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/admin/rh/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pending HR accounts"))
                .andExpect(jsonPath("$.data[0].username").value("rh1"));
    }

    @Test
    void testApproveRh() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "rh1", "firstName", "lastName", "rh1@test.com", null, true, null, null, null);
        Mockito.when(adminService.approveHr(1L)).thenReturn(userDTO);

        mockMvc.perform(put("/api/admin/rh/1/approve")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("HR approved"))
                .andExpect(jsonPath("$.data.username").value("rh1"));
    }
    
    @Test
    void testPendingHr() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "rh1", "firstName", "lastName", "rh1@test.com", null, true, null, null, null);
        Mockito.when(adminService.listPendingHr()).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/admin/hr/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pending HR accounts"))
                .andExpect(jsonPath("$.data[0].username").value("rh1"));
    }

    @Test
    void testApproveHr() throws Exception {
        UserDTO userDTO = new UserDTO(1L, "rh1", "firstName", "lastName", "rh1@test.com", null, true, null, null, null);
        Mockito.when(adminService.approveHr(1L)).thenReturn(userDTO);

        mockMvc.perform(put("/api/admin/hr/1/approve")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("HR approved"))
                .andExpect(jsonPath("$.data.username").value("rh1"));
    }
}
