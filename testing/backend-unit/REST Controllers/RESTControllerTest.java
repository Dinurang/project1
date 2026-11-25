import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.suntravels.model.RoomType;
import com.suntravels.model.SearchRequest;
import com.suntravels.model.SearchResult;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import com.suntravels.service.AuthService;
import com.suntravels.service.SearchService;

@ExtendWith(MockitoExtension.class)
class RESTControllerTest {
    // Verifies HTTP request binding and JSON responses while services and repositories are mocked.

    @Mock
    private ContractRepository contractRepo;

    @Mock
    private ContractAdministratorRepository adminRepo;

    @Mock
    private SearchService searchService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminContractController adminContractController;

    @InjectMocks
    private AgentSearchController agentSearchController;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ContractAdministrator sampleAdmin;
    private Contract sampleContract;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                adminContractController,
                agentSearchController,
                authController
        ).build();

        sampleAdmin = new ContractAdministrator();
        sampleAdmin.setId(5L);
        sampleAdmin.setFullName("Kasun Jayawardena");
        sampleAdmin.setEmployeeCode("EMP-105");
        sampleAdmin.setContactNo("0771234567");

        RoomType room = new RoomType();
        room.setId(10L);
        room.setTypeName("Ocean Suite");
        room.setPrice(new BigDecimal("250.00"));
        room.setNoOfRooms(6);
        room.setMaxAdults(3);

        sampleContract = new Contract();
        sampleContract.setId(100L);
        sampleContract.setHotelName("Shangri-La Hambantota");
        sampleContract.setValidFrom(LocalDate.of(2026, 12, 1));
        sampleContract.setValidTo(LocalDate.of(2026, 12, 31));
        sampleContract.setMarkup(new BigDecimal("18.00"));
        sampleContract.setAdministrator(sampleAdmin);
        sampleContract.setRoomTypes(new java.util.ArrayList<>(List.of(room)));
    }

    // -------------------------------------------------------------------------
    // AdminContractController (/api/admin/contracts)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CTRL-01: POST /api/admin/contracts binds authenticated administrator and returns saved Contract JSON")
    void shouldCreateContractAndBindAuthenticatedAdmin() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("kasun_admin");
        when(adminRepo.findByUserUsername("kasun_admin")).thenReturn(Optional.of(sampleAdmin));
        when(contractRepo.save(any(Contract.class))).thenReturn(sampleContract);

        String requestBody = """
                {
                  "hotelName": "Shangri-La Hambantota",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 18.00,
                  "roomTypes": [
                    {
                      "typeName": "Ocean Suite",
                      "price": 250.00,
                      "noOfRooms": 6,
                      "maxAdults": 3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/contracts")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.hotelName").value("Shangri-La Hambantota"))
                .andExpect(jsonPath("$.administrator.fullName").value("Kasun Jayawardena"))
                .andExpect(jsonPath("$.administrator.employeeCode").value("EMP-105"))
                .andExpect(jsonPath("$.roomTypes[0].typeName").value("Ocean Suite"));
    }

    @Test
    @DisplayName("TC-CTRL-02: GET /api/admin/contracts returns full list of contracts")
    void shouldReturnAllContracts() throws Exception {
        when(contractRepo.findAll()).thenReturn(List.of(sampleContract));

        mockMvc.perform(get("/api/admin/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].hotelName").value("Shangri-La Hambantota"))
                .andExpect(jsonPath("$[0].administrator.fullName").value("Kasun Jayawardena"));
    }

    @Test
    @DisplayName("TC-CTRL-03: GET /api/admin/contracts/search filters by hotelName and ISO activeDate query params")
    void shouldSearchContractsByHotelNameAndActiveDate() throws Exception {
        when(contractRepo.findAll()).thenReturn(List.of(sampleContract));

        mockMvc.perform(get("/api/admin/contracts/search")
                        .param("hotelName", "Shangri")
                        .param("activeDate", "2026-12-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));

        // Out-of-range activeDate returns empty JSON array
        mockMvc.perform(get("/api/admin/contracts/search")
                        .param("hotelName", "Shangri")
                        .param("activeDate", "2027-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-CTRL-08: PUT /api/admin/contracts/{id} updates contract and replaces room types")
    void shouldUpdateContractAndRoomTypes() throws Exception {
        when(contractRepo.findById(100L)).thenReturn(Optional.of(sampleContract));
        when(contractRepo.save(any(Contract.class))).thenReturn(sampleContract);

        mockMvc.perform(put("/api/admin/contracts/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hotelName": "Updated Hotel",
                                  "validFrom": "2027-01-01",
                                  "validTo": "2027-01-31",
                                  "markup": 20.00,
                                  "roomTypes": [
                                    {
                                      "typeName": "Updated Suite",
                                      "price": 300.00,
                                      "noOfRooms": 4,
                                      "maxAdults": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.hotelName").value("Updated Hotel"))
                .andExpect(jsonPath("$.administrator.fullName").value("Kasun Jayawardena"))
                .andExpect(jsonPath("$.roomTypes.length()").value(1))
                .andExpect(jsonPath("$.roomTypes[0].typeName").value("Updated Suite"));

        verify(contractRepo).save(sampleContract);
    }

    @Test
    @DisplayName("TC-CTRL-09: PUT /api/admin/contracts/{id} returns 400 when no room types are supplied")
    void shouldRejectContractUpdateWithoutRoomTypes() throws Exception {
        when(contractRepo.findById(100L)).thenReturn(Optional.of(sampleContract));

        mockMvc.perform(put("/api/admin/contracts/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hotelName": "Updated Hotel",
                                  "validFrom": "2027-01-01",
                                  "validTo": "2027-01-31",
                                  "markup": 20.00,
                                  "roomTypes": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CTRL-10: PUT and DELETE /api/admin/contracts/{id} return 404 for a missing contract")
    void shouldReturnNotFoundWhenUpdatingOrDeletingMissingContract() throws Exception {
        when(contractRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/contracts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hotelName": "Missing",
                                  "validFrom": "2027-01-01",
                                  "validTo": "2027-01-31",
                                  "markup": 20.00,
                                  "roomTypes": [
                                    { "typeName": "Suite", "price": 300, "noOfRooms": 4, "maxAdults": 2 }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/admin/contracts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-CTRL-11: DELETE /api/admin/contracts/{id} removes an existing contract")
    void shouldDeleteContract() throws Exception {
        when(contractRepo.findById(100L)).thenReturn(Optional.of(sampleContract));

        mockMvc.perform(delete("/api/admin/contracts/100"))
                .andExpect(status().isNoContent());

        verify(contractRepo).delete(sampleContract);
    }

    // -------------------------------------------------------------------------
    // AgentSearchController (/api/agent/search)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CTRL-04: POST /api/agent/search parses SearchRequest JSON and returns SearchResult list")
    void shouldReturnAvailableSearchResultsForAgentRequest() throws Exception {
        SearchResult result = new SearchResult(
                "Shangri-La Hambantota",
                "Ocean Suite",
                new BigDecimal("885.00"),
                "Available"
        );
        when(searchService.search(any(SearchRequest.class))).thenReturn(List.of(result));

        String searchPayload = """
                {
                  "checkInDate": "2026-12-10",
                  "noOfNights": 3,
                  "roomRequests": [
                    { "adults": 2 },
                    { "adults": 1 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/agent/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].hotelName").value("Shangri-La Hambantota"))
                .andExpect(jsonPath("$[0].roomType").value("Ocean Suite"))
                .andExpect(jsonPath("$[0].price").value(885.0))
                .andExpect(jsonPath("$[0].status").value("Available"));

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("TC-CTRL-05: POST /api/agent/search returns empty JSON array when no rooms are available")
    void shouldReturnEmptyListWhenNoRoomsMatchSearch() throws Exception {
        when(searchService.search(any(SearchRequest.class))).thenReturn(List.of());

        String searchPayload = """
                {
                  "checkInDate": "2026-12-10",
                  "noOfNights": 2,
                  "roomRequests": [
                    { "adults": 6 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/agent/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // AuthController (/api/auth/signup, /api/auth/login)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-CTRL-06: POST /api/auth/signup returns 200 on success and 400 on duplicate/invalid input")
    void shouldHandleSignupSuccessAndBadRequest() throws Exception {
        doNothing().when(authService).signup(any(SignupRequest.class));

        String validSignup = """
                {
                  "username": "new_agent",
                  "email": "new_agent@suntravels.com",
                  "password": "Password123!",
                  "role": "CALL_CENTER_AGENT"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Please log in."));

        doThrow(new IllegalArgumentException("Username already exists"))
                .when(authService).signup(any(SignupRequest.class));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignup))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    @DisplayName("TC-CTRL-07: POST /api/auth/login returns 200 on valid credentials and 401 on invalid login")
    void shouldHandleLoginSuccessAndUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(null);

        String loginPayload = """
                {
                  "username": "kasun_admin",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk());

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }
}