import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suntravels.model.Contract;
import com.suntravels.model.RoomRequest;
import com.suntravels.model.RoomType;
import com.suntravels.model.SearchRequest;
import com.suntravels.model.SearchResult;
import com.suntravels.repository.ContractRepository;
import com.suntravels.service.SearchService;

@ExtendWith(MockitoExtension.class)
class BusinessLogicTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private SearchService searchService;

    private Contract cinnamonContract;
    private RoomType standardDouble;
    private RoomType familySuite;

    @BeforeEach
    void setUp() {
        cinnamonContract = new Contract();
        cinnamonContract.setId(1L);
        cinnamonContract.setHotelName("Cinnamon Grand Colombo");
        cinnamonContract.setValidFrom(LocalDate.of(2026, 12, 1));
        cinnamonContract.setValidTo(LocalDate.of(2026, 12, 31));
        cinnamonContract.setMarkup(new BigDecimal("15.00")); // 1.15x multiplier

        // RoomType 1: 5 rooms available, max 2 adults per room, $100.00/adult/night
        standardDouble = new RoomType();
        standardDouble.setId(10L);
        standardDouble.setTypeName("Standard Double");
        standardDouble.setPrice(new BigDecimal("100.00"));
        standardDouble.setNoOfRooms(5);
        standardDouble.setMaxAdults(2);

        // RoomType 2: 2 rooms available, max 4 adults per room, $200.00/adult/night
        familySuite = new RoomType();
        familySuite.setId(11L);
        familySuite.setTypeName("Family Suite");
        familySuite.setPrice(new BigDecimal("200.00"));
        familySuite.setNoOfRooms(2);
        familySuite.setMaxAdults(4);

        cinnamonContract.setRoomTypes(List.of(standardDouble, familySuite));
    }

    private SearchRequest buildSearchRequest(LocalDate checkIn, Integer nights, List<RoomRequest> roomRequests) {
        SearchRequest request = new SearchRequest();
        request.setCheckInDate(checkIn);
        request.setNoOfNights(nights);
        request.setRoomRequests(roomRequests);
        return request;
    }

    // -------------------------------------------------------------------------
    // 1. Availability computation (sufficient room count & per-room adult capacity)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-01: Returns 'Available' only for room types meeting both room count and every room's adult capacity")
    void shouldComputeAvailabilityBasedOnRoomCountAndAdultCapacity() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate expectedCheckOut = LocalDate.of(2026, 12, 13);

        // 2 rooms requested: Room 1 needs 3 adults, Room 2 needs 2 adults
        SearchRequest request = buildSearchRequest(
                checkIn,
                3,
                List.of(new RoomRequest(3), new RoomRequest(2))
        );

        when(contractRepository.findValidContracts(checkIn, expectedCheckOut))
                .thenReturn(List.of(cinnamonContract));

        List<SearchResult> results = searchService.search(request);

        // Standard Double (maxAdults=2) fails Room 1 (3 adults); Family Suite (2 rooms >= 2, maxAdults=4 >= 3) succeeds
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHotelName()).isEqualTo("Cinnamon Grand Colombo");
        assertThat(results.get(0).getRoomType()).isEqualTo("Family Suite");
        assertThat(results.get(0).getStatus()).isEqualTo("Available");
    }

    @Test
    @DisplayName("TC-BL-02: Returns empty list when requested room count or adult capacity cannot be satisfied")
    void shouldReturnEmptyWhenRoomsOrCapacityAreInsufficient() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate expectedCheckOut = LocalDate.of(2026, 12, 12);

        when(contractRepository.findValidContracts(checkIn, expectedCheckOut))
                .thenReturn(List.of(cinnamonContract));

        // Case A: 6 rooms requested (exceeds Standard Double's 5 and Family Suite's 2)
        SearchRequest tooManyRooms = buildSearchRequest(
                checkIn,
                2,
                List.of(
                        new RoomRequest(1), new RoomRequest(1), new RoomRequest(1),
                        new RoomRequest(1), new RoomRequest(1), new RoomRequest(1)
                )
        );

        // Case B: 1 room with 5 adults requested (exceeds maxAdults=4)
        SearchRequest tooManyAdults = buildSearchRequest(
                checkIn,
                2,
                List.of(new RoomRequest(5))
        );

        assertThat(searchService.search(tooManyRooms)).isEmpty();
        assertThat(searchService.search(tooManyAdults)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 2. Number of nights calculation (checkOut = checkIn + noOfNights)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-03: Computes checkOutDate as checkInDate + noOfNights and scales cost by nights")
    void shouldCalculateCheckOutDateFromNightsAndScaleCost() {
        LocalDate checkIn = LocalDate.of(2026, 12, 5);
        int nights = 6;
        LocalDate expectedCheckOut = LocalDate.of(2026, 12, 11);

        // 1 room, 1 adult, 6 nights on Standard Double ($100.00, 15% markup)
        SearchRequest request = buildSearchRequest(checkIn, nights, List.of(new RoomRequest(1)));

        when(contractRepository.findValidContracts(checkIn, expectedCheckOut))
                .thenReturn(List.of(cinnamonContract));

        List<SearchResult> results = searchService.search(request);

        // Verifies SearchService computed checkOut = Dec 5 + 6 days = Dec 11
        verify(contractRepository).findValidContracts(checkIn, expectedCheckOut);

        // Cost = $100.00 * 1 adult * 6 nights * 1.15 = $690.00
        assertThat(results).isNotEmpty();
        SearchResult standardResult = results.stream()
                .filter(r -> "Standard Double".equals(r.getRoomType()))
                .findFirst()
                .orElseThrow();
        assertThat(standardResult.getPrice()).isEqualByComparingTo("690.00");
    }

    // -------------------------------------------------------------------------
    // 3. Cost calculation - number of persons (single room, 1 night, varying adults)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-04: Calculates total cost accurately based on number of persons in a room")
    void shouldCalculateCostBasedOnNumberOfPersons() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 11);

        // 1 room with 3 adults for 1 night -> only Family Suite ($200.00/adult/night, 15% markup) fits 3 adults
        SearchRequest request = buildSearchRequest(checkIn, 1, List.of(new RoomRequest(3)));

        when(contractRepository.findValidContracts(checkIn, checkOut))
                .thenReturn(List.of(cinnamonContract));

        List<SearchResult> results = searchService.search(request);

        // Cost = $200.00 * 3 persons * 1 night * 1.15 = $690.00
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPrice()).isEqualByComparingTo("690.00");
    }

    // -------------------------------------------------------------------------
    // 4. Cost calculation - number of rooms (multiple rooms, 1 person each, 1 night)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-05: Calculates total cost accurately across multiple requested rooms")
    void shouldCalculateCostBasedOnNumberOfRooms() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 11);

        // 4 rooms with 1 adult each for 1 night -> only Standard Double (5 rooms, $100.00/adult/night, 15% markup) has >= 4 rooms
        SearchRequest request = buildSearchRequest(
                checkIn,
                1,
                List.of(new RoomRequest(1), new RoomRequest(1), new RoomRequest(1), new RoomRequest(1))
        );

        when(contractRepository.findValidContracts(checkIn, checkOut))
                .thenReturn(List.of(cinnamonContract));

        List<SearchResult> results = searchService.search(request);

        // Total adults across 4 rooms = 4; Cost = $100.00 * 4 * 1 night * 1.15 = $460.00
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomType()).isEqualTo("Standard Double");
        assertThat(results.get(0).getPrice()).isEqualByComparingTo("460.00");
    }

    // -------------------------------------------------------------------------
    // 5. Cost calculation - combined number of persons AND rooms (with HALF_UP rounding)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-06: Calculates combined cost for multiple rooms, multiple persons, multiple nights, and HALF_UP markup")
    void shouldCalculateCostForCombinedPersonsRoomsAndNightsWithRounding() {
        Contract boutiqueContract = new Contract();
        boutiqueContract.setId(2L);
        boutiqueContract.setHotelName("Jetwing Lighthouse");
        boutiqueContract.setValidFrom(LocalDate.of(2026, 12, 1));
        boutiqueContract.setValidTo(LocalDate.of(2026, 12, 31));
        boutiqueContract.setMarkup(new BigDecimal("13.33")); // 1.133300 multiplier

        RoomType oceanSuite = new RoomType();
        oceanSuite.setId(20L);
        oceanSuite.setTypeName("Ocean Suite");
        oceanSuite.setPrice(new BigDecimal("99.00"));
        oceanSuite.setNoOfRooms(4);
        oceanSuite.setMaxAdults(3);
        boutiqueContract.setRoomTypes(List.of(oceanSuite));

        LocalDate checkIn = LocalDate.of(2026, 12, 15);
        int nights = 3;
        LocalDate checkOut = LocalDate.of(2026, 12, 18);

        // 2 rooms: Room 1 has 3 adults, Room 2 has 2 adults -> totalAdults = 5
        SearchRequest request = buildSearchRequest(
                checkIn,
                nights,
                List.of(new RoomRequest(3), new RoomRequest(2))
        );

        when(contractRepository.findValidContracts(checkIn, checkOut))
                .thenReturn(List.of(boutiqueContract));

        List<SearchResult> results = searchService.search(request);

        // Base = $99.00 * 5 adults * 3 nights = $1,485.00
        // Marked-up = 1485.00 * 1.133300 = 1682.9505 -> HALF_UP to 2 decimals = $1,682.95
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPrice()).isEqualByComparingTo("1682.95");
    }

    // -------------------------------------------------------------------------
    // 6. Null input guard
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC-BL-07: Returns empty list immediately without querying DB when required search fields are null")
    void shouldReturnEmptyWhenRequestFieldsAreNull() {
        SearchRequest invalidRequest = new SearchRequest();

        List<SearchResult> results = searchService.search(invalidRequest);

        assertThat(results).isEmpty();
        verifyNoInteractions(contractRepository);
    }
}