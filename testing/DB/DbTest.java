import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import com.suntravels.SunTravelsApplication;
import com.suntravels.model.CallCenterAgent;
import com.suntravels.model.Contract;
import com.suntravels.model.ContractAdministrator;
import com.suntravels.model.Role;
import com.suntravels.model.RoomRequest;
import com.suntravels.model.RoomType;
import com.suntravels.model.User;
import com.suntravels.repository.CallCenterAgentRepository;
import com.suntravels.repository.ContractAdministratorRepository;
import com.suntravels.repository.ContractRepository;
import com.suntravels.repository.UserRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ContextConfiguration(classes = SunTravelsApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DbTest {
    // Exercises JPA entities and repositories against the configured MySQL test database.

    @Autowired
    private TestEntityManager testEntityManager;

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

    private ContractAdministrator defaultAdmin;

    @BeforeEach
    void setUp() {
        User baseAdminUser = new User();
        baseAdminUser.setUsername("base_contract_admin");
        baseAdminUser.setEmail("base_admin@suntravels.com");
        baseAdminUser.setPasswordHash("$2a$10$hashedPasswordSample1234567890123456789012");
        baseAdminUser.setRole(Role.CONTRACT_ADMIN);
        userRepository.saveAndFlush(baseAdminUser);

        ContractAdministrator admin = new ContractAdministrator();
        admin.setFullName("Nimal Perera");
        admin.setEmployeeCode("EMP-BASE-001");
        admin.setContactNo("0771234567");
        admin.setUser(baseAdminUser);
        defaultAdmin = adminRepository.saveAndFlush(admin);
    }

    private Contract buildContract(String hotelName, LocalDate validFrom, LocalDate validTo,
                                   BigDecimal markup, ContractAdministrator creator) {
        Contract contract = new Contract();
        contract.setHotelName(hotelName);
        contract.setValidFrom(validFrom);
        contract.setValidTo(validTo);
        contract.setMarkup(markup);
        contract.setAdministrator(creator);

        RoomType deluxe = new RoomType();
        deluxe.setTypeName("Deluxe Ocean View");
        deluxe.setPrice(new BigDecimal("180.00"));
        deluxe.setNoOfRooms(10);
        deluxe.setMaxAdults(2);

        RoomType suite = new RoomType();
        suite.setTypeName("Family Suite");
        suite.setPrice(new BigDecimal("320.00"));
        suite.setNoOfRooms(4);
        suite.setMaxAdults(4);

        contract.setRoomTypes(new ArrayList<>(List.of(deluxe, suite)));
        return contract;
    }

    // -------------------------------------------------------------------------
    // 1. Insert to user and contract_administrator
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-01: Insert into user and contract_administrator and verify via repositories")
    void shouldInsertUserAndContractAdministrator() {
        User user = new User();
        user.setUsername("new_admin_user");
        user.setEmail("new_admin@suntravels.com");
        user.setPasswordHash("$2a$10$hashedAdminPassword12345678901234567890123");
        user.setRole(Role.CONTRACT_ADMIN);
        User savedUser = userRepository.saveAndFlush(user);

        ContractAdministrator admin = new ContractAdministrator();
        admin.setFullName("Kavindi Silva");
        admin.setEmployeeCode("EMP-NEW-101");
        admin.setContactNo("0712345678");
        admin.setUser(savedUser);
        ContractAdministrator savedAdmin = adminRepository.saveAndFlush(admin);

        testEntityManager.clear();

        Optional<ContractAdministrator> found = adminRepository.findByUserUsername("new_admin_user");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedAdmin.getId());
        assertThat(found.get().getEmployeeCode()).isEqualTo("EMP-NEW-101");
        assertThat(found.get().getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(found.get().getUser().getRole()).isEqualTo(Role.CONTRACT_ADMIN);
        assertThat(adminRepository.existsByEmployeeCode("EMP-NEW-101")).isTrue();
    }

    // -------------------------------------------------------------------------
    // 2. Insert to user and call_center_agent
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-02: Insert into user and call_center_agent and verify via repositories")
    void shouldInsertUserAndCallCenterAgent() {
        User user = new User();
        user.setUsername("agent_kasun");
        user.setEmail("kasun@suntravels.com");
        user.setPasswordHash("$2a$10$hashedAgentPassword12345678901234567890123");
        user.setRole(Role.CALL_CENTER_AGENT);
        User savedUser = userRepository.saveAndFlush(user);

        CallCenterAgent agent = new CallCenterAgent();
        agent.setFullName("Kasun Fernando");
        agent.setAgentCode("AGT-2026-01");
        agent.setDeskExtension("EXT-405");
        agent.setUser(savedUser);
        CallCenterAgent savedAgent = agentRepository.saveAndFlush(agent);

        testEntityManager.clear();

        Optional<CallCenterAgent> found = agentRepository.findByUserUsername("agent_kasun");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedAgent.getId());
        assertThat(found.get().getFullName()).isEqualTo("Kasun Fernando");
        assertThat(found.get().getAgentCode()).isEqualTo("AGT-2026-01");
        assertThat(found.get().getDeskExtension()).isEqualTo("EXT-405");
        assertThat(found.get().getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(found.get().getUser().getRole()).isEqualTo(Role.CALL_CENTER_AGENT);
        assertThat(agentRepository.existsByAgentCode("AGT-2026-01")).isTrue();
    }

    // -------------------------------------------------------------------------
    // 3. Insert a contract
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-03: Insert a contract and verify persisted fields")
    void shouldInsertContract() {
        Contract contract = buildContract(
                "Cinnamon Grand Colombo",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("15.50"),
                defaultAdmin
        );

        Contract saved = contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        Contract reloaded = contractRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getHotelName()).isEqualTo("Cinnamon Grand Colombo");
        assertThat(reloaded.getValidFrom()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(reloaded.getValidTo()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(reloaded.getMarkup()).isEqualByComparingTo("15.50");
    }

    // -------------------------------------------------------------------------
    // 4. Insertion to contract records its contract_administrator creator
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-04: Inserting a contract records its contract_administrator creator (admin_id)")
    void shouldRecordContractAdministratorCreatorOnInsert() {
        Contract contract = buildContract(
                "Shangri-La Hambantota",
                LocalDate.of(2026, 11, 25),
                LocalDate.of(2027, 2, 28),
                new BigDecimal("18.00"),
                defaultAdmin
        );

        Contract saved = contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        Contract reloaded = contractRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAdministrator()).isNotNull();
        assertThat(reloaded.getAdministrator().getId()).isEqualTo(defaultAdmin.getId());
        assertThat(reloaded.getAdministrator().getEmployeeCode()).isEqualTo("EMP-BASE-001");

        Number dbAdminId = (Number) entityManager.createNativeQuery(
                        "SELECT admin_id FROM contract WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(dbAdminId.longValue()).isEqualTo(defaultAdmin.getId());
    }

    // -------------------------------------------------------------------------
    // 5. Insertion of contract inserts to room_type simultaneously
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-05: Inserting a contract simultaneously cascades inserts to room_type")
    void shouldInsertRoomTypesSimultaneouslyWithContract() {
        Contract contract = buildContract(
                "Jetwing Blue",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2027, 4, 30),
                new BigDecimal("14.00"),
                defaultAdmin
        );

        Contract saved = contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        Contract reloaded = contractRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRoomTypes()).hasSize(2);
        assertThat(reloaded.getRoomTypes()).allMatch(rt -> rt.getId() != null);
        assertThat(reloaded.getRoomTypes())
                .extracting(RoomType::getTypeName)
                .containsExactlyInAnyOrder("Deluxe Ocean View", "Family Suite");
    }

    // -------------------------------------------------------------------------
    // 6. Date range inclusion for search queries (ContractRepository.findValidContracts)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-06: findValidContracts includes exact boundary dates and interior stay dates")
    void shouldIncludeContractsWithinValidDateRange() {
        Contract contract = buildContract(
                "Heritance Ahungalla",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("12.50"),
                defaultAdmin
        );
        contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        List<Contract> exactBoundaryResults = contractRepository.findValidContracts(
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31)
        );

        List<Contract> interiorResults = contractRepository.findValidContracts(
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15)
        );

        assertThat(exactBoundaryResults).extracting(Contract::getHotelName).contains("Heritance Ahungalla");
        assertThat(interiorResults).extracting(Contract::getHotelName).contains("Heritance Ahungalla");
    }

    // -------------------------------------------------------------------------
    // 7. Expired contract date (ContractRepository.findValidContracts)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-07: findValidContracts excludes expired contracts and contracts expiring mid-stay")
    void shouldExcludeExpiredContracts() {
        Contract fullyExpired = buildContract(
                "Galle Face Hotel",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31),
                new BigDecimal("10.00"),
                defaultAdmin
        );
        Contract expiresMidStay = buildContract(
                "Mount Lavinia Hotel",
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 12, 12),
                new BigDecimal("11.00"),
                defaultAdmin
        );
        contractRepository.saveAll(List.of(fullyExpired, expiresMidStay));
        testEntityManager.flush();
        testEntityManager.clear();

        List<Contract> results = contractRepository.findValidContracts(
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15)
        );

        assertThat(results)
                .extracting(Contract::getHotelName)
                .doesNotContain("Galle Face Hotel", "Mount Lavinia Hotel");
    }

    // -------------------------------------------------------------------------
    // 8. Future contract date (ContractRepository.findValidContracts)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-08: findValidContracts excludes future contracts whose validFrom is after check-in")
    void shouldExcludeFutureContracts() {
        Contract futureContract = buildContract(
                "Amaya Hills Kandy",
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 5, 31),
                new BigDecimal("16.00"),
                defaultAdmin
        );
        Contract startsMidStay = buildContract(
                "Earl's Regency",
                LocalDate.of(2026, 12, 12),
                LocalDate.of(2027, 3, 31),
                new BigDecimal("15.00"),
                defaultAdmin
        );
        contractRepository.saveAll(List.of(futureContract, startsMidStay));
        testEntityManager.flush();
        testEntityManager.clear();

        List<Contract> results = contractRepository.findValidContracts(
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15)
        );

        assertThat(results)
                .extracting(Contract::getHotelName)
                .doesNotContain("Amaya Hills Kandy", "Earl's Regency");
    }

    // -------------------------------------------------------------------------
    // 9. 'Available' outcomes - sufficient room count & capacity >= guest request
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-09: Returns 'Available' room types when noOfRooms >= requestedRooms and maxAdults >= guest request")
    void shouldReturnAvailableRoomsMeetingRoomCountAndAdultCapacity() {
        Contract contract = buildContract(
                "The Kingsbury",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("20.00"),
                defaultAdmin
        );
        // Deluxe: 10 rooms, max 2 adults | Family Suite: 4 rooms, max 4 adults
        contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 14);
        List<RoomRequest> guestRequests = List.of(
                new RoomRequest(3),
                new RoomRequest(2),
                new RoomRequest(3)
        );
        int requestedRoomCount = guestRequests.size();
        int maxRequestedAdults = guestRequests.stream().mapToInt(RoomRequest::getAdults).max().orElse(1);

        List<Contract> validContracts = contractRepository.findValidContracts(checkIn, checkOut);

        List<RoomType> availableRooms = validContracts.stream()
                .filter(c -> "The Kingsbury".equals(c.getHotelName()))
                .flatMap(c -> c.getRoomTypes().stream())
                .filter(rt -> rt.getNoOfRooms() >= requestedRoomCount && rt.getMaxAdults() >= maxRequestedAdults)
                .toList();

        // Only Family Suite (4 rooms >= 3, maxAdults 4 >= 3) qualifies; Deluxe (maxAdults 2 < 3) is excluded
        assertThat(availableRooms).hasSize(1);
        assertThat(availableRooms.get(0).getTypeName()).isEqualTo("Family Suite");
        assertThat(availableRooms.get(0).getNoOfRooms()).isGreaterThanOrEqualTo(requestedRoomCount);
        assertThat(availableRooms.get(0).getMaxAdults()).isGreaterThanOrEqualTo(maxRequestedAdults);
    }

    // -------------------------------------------------------------------------
    // 10. 'Unavailable' - No Outcome scenarios
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-10: Returns empty ('Unavailable') when room count or adult capacity is insufficient")
    void shouldReturnUnavailableWhenRoomCountOrCapacityIsInsufficient() {
        Contract contract = buildContract(
                "Anantara Peace Haven",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("22.00"),
                defaultAdmin
        );
        // Max rooms available is 10 (Deluxe) and max adults is 4 (Family Suite)
        contractRepository.saveAndFlush(contract);
        testEntityManager.clear();

        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 14);
        List<Contract> validContracts = contractRepository.findValidContracts(checkIn, checkOut);

        // Scenario A: Requested room count (15) exceeds all room types
        List<RoomType> insufficientRooms = validContracts.stream()
                .filter(c -> "Anantara Peace Haven".equals(c.getHotelName()))
                .flatMap(c -> c.getRoomTypes().stream())
                .filter(rt -> rt.getNoOfRooms() >= 15 && rt.getMaxAdults() >= 2)
                .toList();

        // Scenario B: Requested adult capacity (6) exceeds all room types
        List<RoomType> insufficientCapacity = validContracts.stream()
                .filter(c -> "Anantara Peace Haven".equals(c.getHotelName()))
                .flatMap(c -> c.getRoomTypes().stream())
                .filter(rt -> rt.getNoOfRooms() >= 1 && rt.getMaxAdults() >= 6)
                .toList();

        assertThat(insufficientRooms).isEmpty();
        assertThat(insufficientCapacity).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 11. Read all contracts
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-11: contractRepository.findAll() returns every persisted contract")
    void shouldReadAllContracts() {
        long initialCount = contractRepository.count();

        Contract c1 = buildContract("Hotel Sigiriya", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), new BigDecimal("12.00"), defaultAdmin);
        Contract c2 = buildContract("Jetwing Vil Uyana", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31), new BigDecimal("19.00"), defaultAdmin);
        contractRepository.saveAll(List.of(c1, c2));
        testEntityManager.flush();
        testEntityManager.clear();

        List<Contract> allContracts = contractRepository.findAll();
        assertThat(allContracts).hasSize((int) initialCount + 2);
        assertThat(allContracts)
                .extracting(Contract::getHotelName)
                .contains("Hotel Sigiriya", "Jetwing Vil Uyana");
    }

    // -------------------------------------------------------------------------
    // 12. Read user with login credentials (UserRepository)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-12: userRepository.findByUsername retrieves stored password hash and role")
    void shouldReadUserWithLoginCredentials() {
        User user = new User();
        user.setUsername("login_agent_01");
        user.setEmail("login_agent_01@suntravels.com");
        user.setPasswordHash("$2a$10$expectedBcryptHashValue12345678901234567890");
        user.setRole(Role.CALL_CENTER_AGENT);
        userRepository.saveAndFlush(user);
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findByUsername("login_agent_01");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("login_agent_01");
        assertThat(foundUser.get().getPasswordHash()).isEqualTo("$2a$10$expectedBcryptHashValue12345678901234567890");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.CALL_CENTER_AGENT);
        assertThat(userRepository.existsByUsername("login_agent_01")).isTrue();
        assertThat(userRepository.existsByEmail("login_agent_01@suntravels.com")).isTrue();
    }

    // -------------------------------------------------------------------------
    // 13. Search contracts by hotel name
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-13: Search contracts by hotel name (case-insensitive partial match)")
    void shouldSearchContractsByHotelName() {
        Contract c1 = buildContract("Cinnamon Lakeside", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), new BigDecimal("14.00"), defaultAdmin);
        Contract c2 = buildContract("Cinnamon Bentota Beach", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 28), new BigDecimal("16.00"), defaultAdmin);
        Contract c3 = buildContract("Hilton Colombo", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), new BigDecimal("15.00"), defaultAdmin);
        contractRepository.saveAll(List.of(c1, c2, c3));
        testEntityManager.flush();
        testEntityManager.clear();

        String searchHotel = "cinnamon";
        List<Contract> results = contractRepository.findAll().stream()
                .filter(c -> c.getHotelName().toLowerCase().contains(searchHotel.toLowerCase()))
                .toList();

        assertThat(results)
                .extracting(Contract::getHotelName)
                .containsExactlyInAnyOrder("Cinnamon Lakeside", "Cinnamon Bentota Beach")
                .doesNotContain("Hilton Colombo");
    }

    // -------------------------------------------------------------------------
    // 14. Search contracts by valid date
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-14: Search contracts active on a specific valid date via findValidContracts")
    void shouldSearchContractsByValidDate() {
        Contract activeOnDate = buildContract("Taj Samudra", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20), new BigDecimal("17.00"), defaultAdmin);
        Contract inactiveOnDate = buildContract("Marino Beach", LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 15), new BigDecimal("13.50"), defaultAdmin);
        contractRepository.saveAll(List.of(activeOnDate, inactiveOnDate));
        testEntityManager.flush();
        testEntityManager.clear();

        LocalDate targetDate = LocalDate.of(2026, 12, 15);
        List<Contract> results = contractRepository.findValidContracts(targetDate, targetDate);

        assertThat(results)
                .extracting(Contract::getHotelName)
                .contains("Taj Samudra")
                .doesNotContain("Marino Beach");
    }

    // -------------------------------------------------------------------------
    // 15. Search contracts by hotel name AND valid date
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-DB-15: Search contracts matching both hotel name and valid date")
    void shouldSearchContractsByHotelNameAndValidDate() {
        Contract matchingBoth = buildContract(
                "Jetwing Lighthouse",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("15.00"),
                defaultAdmin
        );
        Contract matchingHotelWrongDate = buildContract(
                "Jetwing Lighthouse",
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 3, 31),
                new BigDecimal("18.00"),
                defaultAdmin
        );
        Contract matchingDateWrongHotel = buildContract(
                "Araliya Beach Resort",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("14.00"),
                defaultAdmin
        );
        contractRepository.saveAll(List.of(matchingBoth, matchingHotelWrongDate, matchingDateWrongHotel));
        testEntityManager.flush();
        testEntityManager.clear();

        LocalDate targetDate = LocalDate.of(2026, 12, 18);
        String searchHotel = "jetwing lighthouse";

        List<Contract> results = contractRepository.findValidContracts(targetDate, targetDate).stream()
                .filter(c -> c.getHotelName().toLowerCase().contains(searchHotel.toLowerCase()))
                .toList();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(matchingBoth.getId());
        assertThat(results.get(0).getMarkup()).isEqualByComparingTo("15.00");
    }
}