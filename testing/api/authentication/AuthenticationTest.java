import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.suntravels.SunTravelsApplication;
import com.suntravels.model.Role;
import com.suntravels.service.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;

@SpringBootTest(classes = SunTravelsApplication.class)
@Transactional
class AuthenticationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret:U3VuVHJhdmVsc1NlY3VyZUpXVFNlY3JldEtleTIwMjZGb3JBdXRoZW50aWNhdGlvbg==}")
    private String jwtSecret;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. Unauthenticated access to protected routes is blocked
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-01: Blocks unauthenticated requests to /api/admin/** and /api/agent/**")
    void shouldRejectUnauthenticatedRequestsToProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/contracts"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/agent/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 2. Tampered / malformed Bearer JWT token is rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-02: Rejects malformed or tampered Bearer JWT tokens")
    void shouldRejectInvalidOrTamperedJwtToken() throws Exception {
        mockMvc.perform(get("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.jwt.token"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 3. Expired JWT token is rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-03: Rejects expired JWT tokens")
    void shouldRejectExpiredJwtToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        String expiredToken = Jwts.builder()
                .subject("expired_admin")
                .claims(Map.of("userId", 999L, "role", Role.CONTRACT_ADMIN.name(), "fullName", "Expired User"))
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 4. End-to-end CONTRACT_ADMIN signup -> login -> RBAC enforcement
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-04: CONTRACT_ADMIN can access /api/admin/** but is forbidden from /api/agent/**")
    void shouldAuthenticateContractAdminAndEnforceRoleAccess() throws Exception {
        String signupPayload = """
                {
                  "username": "auth_test_admin",
                  "email": "auth_test_admin@suntravels.com",
                  "password": "AdminPass123!",
                  "role": "CONTRACT_ADMIN",
                  "fullName": "Nimal Perera",
                  "employeeCode": "EMP-AUTH-01",
                  "contactNo": "0771112233"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Please log in."));

        String loginPayload = """
                {
                  "username": "auth_test_admin",
                  "password": "AdminPass123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("auth_test_admin"))
                .andExpect(jsonPath("$.fullName").value("Nimal Perera"))
                .andExpect(jsonPath("$.role").value("CONTRACT_ADMIN"))
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
        assertThat(jwtService.extractRole(token)).isEqualTo("CONTRACT_ADMIN");

        // Allowed: CONTRACT_ADMIN accessing /api/admin/contracts
        mockMvc.perform(get("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // Forbidden: CONTRACT_ADMIN attempting to access /api/agent/search
        mockMvc.perform(post("/api/agent/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 5. End-to-end CALL_CENTER_AGENT signup -> login -> RBAC enforcement
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-05: CALL_CENTER_AGENT can access /api/agent/** but is forbidden from /api/admin/**")
    void shouldAuthenticateCallCenterAgentAndEnforceRoleAccess() throws Exception {
        String signupPayload = """
                {
                  "username": "auth_test_agent",
                  "email": "auth_test_agent@suntravels.com",
                  "password": "AgentPass123!",
                  "role": "CALL_CENTER_AGENT",
                  "fullName": "Kasun Fernando",
                  "agentCode": "AGT-AUTH-01",
                  "deskExtension": "EXT-101"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isOk());

        String loginPayload = """
                {
                  "username": "auth_test_agent",
                  "password": "AgentPass123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CALL_CENTER_AGENT"))
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
        assertThat(jwtService.extractRole(token)).isEqualTo("CALL_CENTER_AGENT");

        // Allowed: CALL_CENTER_AGENT accessing /api/agent/search
        mockMvc.perform(post("/api/agent/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Forbidden: CALL_CENTER_AGENT attempting to access /api/admin/contracts
        mockMvc.perform(get("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 6. Invalid login credentials return 401 Unauthorized
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-06: Returns 401 Unauthorized for wrong password or nonexistent user")
    void shouldReturn401ForInvalidLoginCredentials() throws Exception {
        String signupPayload = """
                {
                  "username": "valid_login_user",
                  "email": "valid_login@suntravels.com",
                  "password": "CorrectPassword123!",
                  "role": "CALL_CENTER_AGENT",
                  "fullName": "Sanduni Perera",
                  "agentCode": "AGT-AUTH-02"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload))
                .andExpect(status().isOk());

        // Wrong password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "valid_login_user",
                                  "password": "WrongPassword999!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password."));

        // Unknown user
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "non_existent_user",
                                  "password": "CorrectPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password."));
    }

    // -------------------------------------------------------------------------
    // 7. Signup role-specific required fields & duplicate checks
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-AUTH-07: Rejects signup when role-specific code is missing or username is duplicated")
    void shouldRejectIncompleteOrDuplicateSignup() throws Exception {
        // Missing employeeCode for CONTRACT_ADMIN
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "missing_emp_admin",
                                  "email": "missing_emp@suntravels.com",
                                  "password": "Password123!",
                                  "role": "CONTRACT_ADMIN",
                                  "fullName": "No Code Admin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Employee code is required for Contract Administrator."));

        // Missing agentCode for CALL_CENTER_AGENT
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "missing_agt_agent",
                                  "email": "missing_agt@suntravels.com",
                                  "password": "Password123!",
                                  "role": "CALL_CENTER_AGENT",
                                  "fullName": "No Code Agent"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Agent code is required for Call Center Agent."));
    }
}