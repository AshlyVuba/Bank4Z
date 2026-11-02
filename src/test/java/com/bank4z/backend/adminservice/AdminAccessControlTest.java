package com.bank4z.backend.adminservice;

import com.bank4z.backend.authservice.CustomUserDetailsService;
import com.bank4z.backend.common.config.SecurityConfig;
import com.bank4z.backend.common.security.JsonAccessDeniedHandler;
import com.bank4z.backend.common.security.JsonAuthenticationEntryPoint;
import com.bank4z.backend.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loads only the web layer plus our real SecurityConfig, so these tests
 * exercise the actual access rules without needing a database.
 */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AdminAccessControlTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminService adminService;

    // Wired into the security setup, so the test context needs stand-ins
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @Test
    void anonymousRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserIsRejectedWith403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotFreezeAnAccount() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/4Z00000001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        when(adminService.listUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }
}