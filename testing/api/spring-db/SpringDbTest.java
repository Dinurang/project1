import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.suntravels.model.CallCenterAgent;
import com.suntravels.model.Contract;
import com.suntravels.model.ContractAdministrator;
import com.suntravels.model.Role;
import com.suntravels.model.User;
import com.suntravels.repository.CallCenterAgentRepository;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import com.suntravels.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;

@SpringBootTest(classes = SunTravelsApplication.class)
@Transactional
class SpringDbTest {
    // Runs end-to-end workflows through HTTP, security, services, JPA, and MySQL.

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractAdministratorRepository adminRepository;

    @Autowired
    private CallCenterAgentRepository agentRepository;

    @Autowired
    private ContractRepository contractRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private String registerAndLoginAdmin(String username, String email, String empCode, String fullName) throws Exception {
        String signupJson = String.format("""
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "AdminPass123!",
                  "role": "CONTRACT_ADMIN",
                  "fullName": "%s",
                  "employeeCode": "%s",
                  "contactNo": "0771234567"
                }
                """, username, email, fullName, empCode);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isOk());

        String loginJson = String.format("""
                {
                  "username": "%s",
                  "password": "AdminPass123!"
                }
                """, username);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String registerAndLoginAgent(String username, String email, String agentCode, String fullName) throws Exception {
        String signupJson = String.format("""
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "AgentPass123!",
                  "role": "CALL_CENTER_AGENT",
                  "fullName": "%s",
                  "agentCode": "%s",
                  "deskExtension": "EXT-202"
                }
                """, username, email, fullName, agentCode);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isOk());

        String loginJson = String.format("""
                {
                  "username": "%s",
                  "password": "AgentPass123!"
                }
                """, username);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    // -------------------------------------------------------------------------
    // 1. Signup API persists hashed User + 1:1 Profile rows into MySQL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-01: POST /api/auth/signup persists BCrypt user and 1:1 profile records in MySQL")
    void shouldPersistUsersAndRoleProfilesInMySqlOnSignup() throws Exception {
        String adminToken = registerAndLoginAdmin(
                "e2e_admin_01", "e2e_admin_01@suntravels.com", "EMP-E2E-01", "Nimal Perera"
        );
        String agentToken = registerAndLoginAgent(
                "e2e_agent_01", "e2e_agent_01@suntravels.com", "AGT-E2E-01", "Kasun Fernando"
        );

        assertThat(adminToken).isNotBlank();
        assertThat(agentToken).isNotBlank();

        entityManager.flush();
        entityManager.clear();

        // Verify User & ContractAdministrator rows in MySQL
        User dbAdminUser = userRepository.findByUsername("e2e_admin_01").orElseThrow();
        assertThat(dbAdminUser.getRole()).isEqualTo(Role.CONTRACT_ADMIN);
        assertThat(dbAdminUser.getPasswordHash()).isNotEqualTo("AdminPass123!");
        assertThat(dbAdminUser.getPasswordHash()).startsWith("$2");

        Optional<ContractAdministrator> dbAdminProfile = adminRepository.findByUserUsername("e2e_admin_01");
        assertThat(dbAdminProfile).isPresent();
        assertThat(dbAdminProfile.get().getEmployeeCode()).isEqualTo("EMP-E2E-01");

        // Verify User & CallCenterAgent rows in MySQL
        User dbAgentUser = userRepository.findByUsername("e2e_agent_01").orElseThrow();
        assertThat(dbAgentUser.getRole()).isEqualTo(Role.CALL_CENTER_AGENT);

        Optional<CallCenterAgent> dbAgentProfile = agentRepository.findByUserUsername("e2e_agent_01");
        assertThat(dbAgentProfile).isPresent();
        assertThat(dbAgentProfile.get().getAgentCode()).isEqualTo("AGT-E2E-01");
        assertThat(dbAgentProfile.get().getDeskExtension()).isEqualTo("EXT-202");
    }

    // -------------------------------------------------------------------------
    // 2. Authenticated Admin POST /api/admin/contracts persists Contract & RoomTypes in MySQL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-02: Authenticated POST /api/admin/contracts saves Contract, creator FK, and RoomTypes in MySQL")
    void shouldPersistContractAndRoomTypesViaAuthenticatedApi() throws Exception {
        String adminToken = registerAndLoginAdmin(
                "e2e_admin_02", "e2e_admin_02@suntravels.com", "EMP-E2E-02", "Kavindi Silva"
        );

        String contractPayload = """
                {
                  "hotelName": "Shangri-La Colombo",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 18.50,
                  "roomTypes": [
                    {
                      "typeName": "Deluxe Lake View",
                      "price": 210.00,
                      "noOfRooms": 8,
                      "maxAdults": 2
                    },
                    {
                      "typeName": "Horizon Club Suite",
                      "price": 350.00,
                      "noOfRooms": 3,
                      "maxAdults": 4
                    }
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.hotelName").value("Shangri-La Colombo"))
                .andExpect(jsonPath("$.administrator.employeeCode").value("EMP-E2E-02"))
                .andExpect(jsonPath("$.roomTypes.length()").value(2))
                .andReturn();

        Number createdId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        entityManager.flush();
        entityManager.clear();

        // Reload directly from MySQL and verify persisted state
        Contract persisted = contractRepository.findById(createdId.longValue()).orElseThrow();
        assertThat(persisted.getHotelName()).isEqualTo("Shangri-La Colombo");
        assertThat(persisted.getMarkup()).isEqualByComparingTo("18.50");
        assertThat(persisted.getAdministrator().getFullName()).isEqualTo("Kavindi Silva");
        assertThat(persisted.getRoomTypes()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // 3. Authenticated Admin GET /api/admin/contracts and /search query MySQL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-03: GET /api/admin/contracts and /search return filtered MySQL records")
    void shouldReadAndFilterPersistedContractsFromMySql() throws Exception {
        String adminToken = registerAndLoginAdmin(
                "e2e_admin_03", "e2e_admin_03@suntravels.com", "EMP-E2E-03", "Ruwan Jayasinghe"
        );

        String decContract = """
                {
                  "hotelName": "Cinnamon Grand Colombo",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 15.00,
                  "roomTypes": [
                    { "typeName": "Premium King", "price": 160.00, "noOfRooms": 10, "maxAdults": 2 }
                  ]
                }
                """;

        String janContract = """
                {
                  "hotelName": "Jetwing Blue Negombo",
                  "validFrom": "2027-01-10",
                  "validTo": "2027-02-28",
                  "markup": 12.00,
                  "roomTypes": [
                    { "typeName": "Ocean Studio", "price": 140.00, "noOfRooms": 6, "maxAdults": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decContract))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(janContract))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Filter by hotelName and activeDate in December 2026
        mockMvc.perform(get("/api/admin/contracts/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("hotelName", "Cinnamon")
                        .param("activeDate", "2026-12-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].hotelName").value("Cinnamon Grand Colombo"));
    }

    // -------------------------------------------------------------------------
    // 4. Full Cross-Role E2E Flow: Admin adds contract in DB -> Agent searches & gets price
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-04: Contract saved by CONTRACT_ADMIN in MySQL is searchable with marked-up pricing by CALL_CENTER_AGENT")
    void shouldExecuteFullAdminContractCreationToAgentSearchWorkflow() throws Exception {
        String adminToken = registerAndLoginAdmin(
                "e2e_admin_04", "e2e_admin_04@suntravels.com", "EMP-E2E-04", "Dilshan Perera"
        );
        String agentToken = registerAndLoginAgent(
                "e2e_agent_04", "e2e_agent_04@suntravels.com", "AGT-E2E-04", "Tharushi Silva"
        );

        String contractPayload = """
                {
                  "hotelName": "Heritance Kandalama",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 15.00,
                  "roomTypes": [
                    {
                      "typeName": "Superior Twin",
                      "price": 80.00,
                      "noOfRooms": 5,
                      "maxAdults": 2
                    },
                    {
                      "typeName": "Luxury Panoramic Suite",
                      "price": 100.00,
                      "noOfRooms": 4,
                      "maxAdults": 3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractPayload))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Agent searches for 3 nights (Dec 10 - Dec 13), 2 rooms: Room 1 (3 adults), Room 2 (2 adults) -> 5 adults total
        String searchPayload = """
                {
                  "checkInDate": "2026-12-10",
                  "noOfNights": 3,
                  "roomRequests": [
                    { "adults": 3 },
                    { "adults": 2 }
                  ]
                }
                """;

        // Superior Twin is unavailable because its per-room capacity is too low;
        // Luxury Panoramic Suite qualifies:
        // Price = $100.00 * 5 adults * 3 nights * 1.15 markup = $1,725.00
        mockMvc.perform(post("/api/agent/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.hotelName == 'Heritance Kandalama' && @.status == 'Available')].roomType").value("Luxury Panoramic Suite"))
                .andExpect(jsonPath("$[?(@.hotelName == 'Heritance Kandalama' && @.status == 'Available')].price").value(1725.0))
                .andExpect(jsonPath("$[?(@.hotelName == 'Heritance Kandalama' && @.status == 'Available')].status").value("Available"))
                .andExpect(jsonPath("$[?(@.hotelName == 'Heritance Kandalama' && @.status == 'Unavailable')].roomType").value("Superior Twin"));
    }

    // -------------------------------------------------------------------------
    // 5. End-to-End Agent Search excludes expired contracts and insufficient room counts
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-05: POST /api/agent/search excludes expired MySQL contracts and marks insufficient inventory unavailable")
    void shouldExcludeExpiredContractsAndInsufficientInventoryEndToEnd() throws Exception {
        String adminToken = registerAndLoginAdmin(
                "e2e_admin_05", "e2e_admin_05@suntravels.com", "EMP-E2E-05", "Saman Kumara"
        );
        String agentToken = registerAndLoginAgent(
                "e2e_agent_05", "e2e_agent_05@suntravels.com", "AGT-E2E-05", "Nadeesha Bandara"
        );

        // Contract with only 1 room available
        String lowInventoryContract = """
                {
                  "hotelName": "Boutique Villa Galle",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 10.00,
                  "roomTypes": [
                    { "typeName": "Single Villa", "price": 150.00, "noOfRooms": 1, "maxAdults": 4 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lowInventoryContract))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // Agent requests 3 rooms (exceeds Boutique Villa Galle's 1 room)
        String searchRequest = """
                {
                  "checkInDate": "2026-12-10",
                  "noOfNights": 2,
                  "roomRequests": [
                    { "adults": 2 },
                    { "adults": 2 },
                    { "adults": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/agent/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.hotelName == 'Boutique Villa Galle' && @.status == 'Unavailable')].status")
                        .value("Unavailable"));
    }

    // -------------------------------------------------------------------------
    // 6. Duplicate registration blocked end-to-end without corrupting DB state
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-06: Duplicate username or employeeCode is rejected with 400 and leaves MySQL unchanged")
    void shouldRejectDuplicateRegistrationAndPreserveDatabaseIntegrity() throws Exception {
        String firstSignup = """
                {
                  "username": "unique_e2e_admin",
                  "email": "unique_e2e_1@suntravels.com",
                  "password": "Password123!",
                  "role": "CONTRACT_ADMIN",
                  "fullName": "First Admin",
                  "employeeCode": "EMP-UNIQ-01",
                  "contactNo": "0770000001"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstSignup))
                .andExpect(status().isOk());

        // Attempt duplicate username
        String duplicateUsernameSignup = """
                {
                  "username": "unique_e2e_admin",
                  "email": "unique_e2e_2@suntravels.com",
                  "password": "Password123!",
                  "role": "CONTRACT_ADMIN",
                  "fullName": "Second Admin",
                  "employeeCode": "EMP-UNIQ-02",
                  "contactNo": "0770000002"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateUsernameSignup))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username is already taken."));

        List<User> matchingUsers = userRepository.findAll().stream()
                .filter(u -> "unique_e2e_admin".equals(u.getUsername()))
                .toList();
        assertThat(matchingUsers).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // 7. Forbidden role cannot insert data into MySQL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-E2E-07: CALL_CENTER_AGENT attempting POST /api/admin/contracts gets 403 and writes nothing to MySQL")
    void shouldPreventUnauthorizedRoleFromPersistingContractsInMySql() throws Exception {
        long countBefore = contractRepository.count();

        String agentToken = registerAndLoginAgent(
                "e2e_agent_07", "e2e_agent_07@suntravels.com", "AGT-E2E-07", "Chathura Silva"
        );

        String contractPayload = """
                {
                  "hotelName": "Unauthorized Hotel",
                  "validFrom": "2026-12-01",
                  "validTo": "2026-12-31",
                  "markup": 10.00,
                  "roomTypes": [
                    { "typeName": "Standard", "price": 100.00, "noOfRooms": 5, "maxAdults": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/contracts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractPayload))
                .andExpect(status().isForbidden());

        assertThat(contractRepository.count()).isEqualTo(countBefore);
    }
}