import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.suntravels.SunTravelsApplication;

import jakarta.servlet.Filter;

@SpringBootTest(classes = SunTravelsApplication.class)
@Transactional
class CorsTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String DISALLOWED_ORIGIN = "http://malicious-site.example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. Preflight OPTIONS request from allowed origin (http://localhost:4200)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-01: Preflight OPTIONS from http://localhost:4200 succeeds with expected CORS headers")
    void shouldAllowPreflightFromConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")));
    }

    // -------------------------------------------------------------------------
    // 2. Preflight OPTIONS on protected admin/agent endpoints is permitted without JWT
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-02: Preflight OPTIONS on JWT-protected /api/admin/contracts succeeds without Bearer token")
    void shouldAllowPreflightOnProtectedRoutesWithoutJwt() throws Exception {
        mockMvc.perform(options("/api/admin/contracts")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    // -------------------------------------------------------------------------
    // 3. Actual cross-origin request from allowed origin includes CORS response headers
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-03: Actual cross-origin request from http://localhost:4200 returns Access-Control-Allow-Origin")
    void shouldIncludeAllowOriginHeaderOnActualCrossOriginRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "cors_check_user",
                                  "password": "AnyPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    // -------------------------------------------------------------------------
    // 4. Disallowed origin is rejected with 403 Forbidden
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-04: Rejects preflight and actual requests from unauthorized origins")
    void shouldRejectDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "cors_check_user",
                                  "password": "AnyPassword123!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // -------------------------------------------------------------------------
    // 5. Disallowed HTTP method in preflight is rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-05: Rejects preflight OPTIONS requesting disallowed HTTP method (PATCH)")
    void shouldRejectDisallowedHttpMethodInPreflight() throws Exception {
        mockMvc.perform(options("/api/admin/contracts")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // -------------------------------------------------------------------------
    // 6. Disallowed request header in preflight is rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CORS-06: Rejects preflight OPTIONS requesting disallowed header (X-Custom-Header)")
    void shouldRejectDisallowedHeaderInPreflight() throws Exception {
        mockMvc.perform(options("/api/agent/search")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Custom-Header"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}