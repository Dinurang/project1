import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.suntravels.controller.AdminContractController;
import com.suntravels.controller.AgentSearchController;
import com.suntravels.controller.AuthController;
import com.suntravels.dto.LoginRequest;
import com.suntravels.dto.SignupRequest;
import com.suntravels.model.Contract;
import com.suntravels.model.ContractAdministrator;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import com.suntravels.service.AuthService;
import com.suntravels.service.SearchService;

@ExtendWith(MockitoExtension.class)
class ExceptionHandlingTest {
    // Verifies that failures from persistence, JSON parsing, validation, and authentication return safe responses.

    @Mock
    private ContractRepository contractRepo;

    @Mock
    private ContractAdministratorRepository adminRepo;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminContractController adminContractController;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AgentSearchController agentSearchController =
                new AgentSearchController(new SearchService(contractRepo));

        mockMvc = MockMvcBuilders.standaloneSetup(
                adminContractController,
                authController,
                agentSearchController
        ).build();
    }

    // -------------------------------------------------------------------------
    // 1. Missing ContractAdministrator profile throws IllegalStateException
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-01: Throws IllegalStateException and aborts save when administrator profile is missing")
    void shouldThrowIllegalStateExceptionWhenAdminProfileNotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("orphan_user");
        when(adminRepo.findByUserUsername("orphan_user")).thenReturn(Optional.empty());

        Contract contract = new Contract();
        contract.setHotelName("Hilton Colombo");
        contract.setValidFrom(LocalDate.of(2026, 12, 1));
        contract.setValidTo(LocalDate.of(2026, 12, 20));
        contract.setMarkup(new BigDecimal("10.00"));

        assertThatThrownBy(() -> adminContractController.addContract(contract, auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Contract Administrator profile not found.");

        verify(contractRepo, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 2. Database persistence failure propagates cleanly
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-02: Propagates DataAccessException when contract repository save fails")
    void shouldPropagateDatabaseExceptionWhenSaveFails() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("valid_admin");

        ContractAdministrator admin = new ContractAdministrator();
        admin.setId(1L);
        admin.setFullName("Nimal Perera");
        when(adminRepo.findByUserUsername("valid_admin")).thenReturn(Optional.of(admin));
        when(contractRepo.save(any(Contract.class)))
                .thenThrow(new DataAccessResourceFailureException("MySQL connection failure"));

        Contract contract = new Contract();
        contract.setHotelName("Cinnamon Grand");
        contract.setValidFrom(LocalDate.of(2026, 12, 1));
        contract.setValidTo(LocalDate.of(2026, 12, 31));
        contract.setMarkup(new BigDecimal("15.00"));

        assertThatThrownBy(() -> adminContractController.addContract(contract, auth))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("MySQL connection failure");
    }

    // -------------------------------------------------------------------------
    // 3. Malformed JSON request bodies return 400 Bad Request
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-03: Returns 400 Bad Request when endpoints receive malformed JSON")
    void shouldReturnBadRequestOnMalformedJsonBody() throws Exception {
        mockMvc.perform(post("/api/admin/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"hotelName\": \"Unclosed JSON "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/agent/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"checkInDate\": \"2026-12-10\", \"noOfNights\": "))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // 4. Invalid ISO date query parameter returns 400 Bad Request
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-04: Returns 400 Bad Request when activeDate query parameter has invalid date format")
    void shouldReturnBadRequestOnInvalidActiveDateFormat() throws Exception {
        mockMvc.perform(get("/api/admin/contracts/search")
                        .param("hotelName", "Cinnamon")
                        .param("activeDate", "20-12-2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(contractRepo);
    }

    // -------------------------------------------------------------------------
    // 5. AuthController maps IllegalArgumentException on signup to HTTP 400
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-05: POST /api/auth/signup catches IllegalArgumentException and returns 400 with error JSON")
    void shouldReturn400WithErrorMessageWhenSignupThrowsIllegalArgument() throws Exception {
        doThrow(new IllegalArgumentException("Email is already registered"))
                .when(authService).signup(any(SignupRequest.class));

        String signupJson = """
                {
                  "username": "kasun_agent",
                  "email": "duplicate@suntravels.com",
                  "password": "Password123!",
                  "role": "CALL_CENTER_AGENT"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    // -------------------------------------------------------------------------
    // 6. AuthController maps IllegalArgumentException on login to HTTP 401
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-06: POST /api/auth/login catches IllegalArgumentException and returns 401 with error JSON")
    void shouldReturn401WithErrorMessageWhenLoginFails() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        String loginJson = """
                {
                  "username": "kasun_agent",
                  "password": "WrongPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    // -------------------------------------------------------------------------
    // 7. Empty/null search payload is handled safely without NullPointerException
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-EX-07: POST /api/agent/search with empty JSON object returns 200 and empty list safely")
    void shouldHandleEmptySearchJsonWithoutNullPointerException() throws Exception {
        mockMvc.perform(post("/api/agent/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verifyNoInteractions(contractRepo);
    }
}