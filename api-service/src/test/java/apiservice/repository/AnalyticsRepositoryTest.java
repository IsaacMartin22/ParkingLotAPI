package apiservice.repository;

import apiservice.dbentity.Analytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import parkinglot.common.model.AnalyticsEventTypes;
import parkinglot.common.request.AnalyticsQuery;
import parkinglot.common.request.AnalyticsQueryFilter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AnalyticsRepository.class)
class AnalyticsRepositoryTest {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    void setUp() {
        analyticsRepository.save(analytics(AnalyticsEventTypes.CLICK, "/home", "Chrome", "Windows", "session-1", "10.0.0.1", "2024-01-01T10:00:00Z"));
        analyticsRepository.save(analytics(AnalyticsEventTypes.PAGE_VIEW, "/pricing", "Firefox", "Linux", "session-2", "10.0.0.2", "2024-01-02T10:00:00Z"));
        analyticsRepository.save(analytics(AnalyticsEventTypes.CLICK, "/home/features", "Chrome", "macOS", "session-3", "10.0.1.5", "2024-01-03T10:00:00Z"));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @ParameterizedTest
    @MethodSource("queryCases")
    void queryReturnsExpectedResults(AnalyticsQuery query, List<Long> expectedIds) {
        AnalyticsQueryResult queryResult = analyticsRepository.query(
                0,
                50,
                query.sortField(),
                query.sortDirection(),
                query.filters()
        );

        assertThat(queryResult.results()).extracting(Analytics::getId).containsExactlyElementsOf(expectedIds);
        assertThat(queryResult.totalCount()).isEqualTo(expectedIds.size());
    }

    @Test
    void querySupportsPagingWithSortAndFilter() {
        AnalyticsQuery query = new AnalyticsQuery(
                List.of(
                        new AnalyticsQueryFilter("browser", "eq", "Chrome")
                ),
                "timestamp",
                "desc",
                1
        );

        AnalyticsQueryResult queryResult = analyticsRepository.query(
                0,
                1,
                query.sortField(),
                query.sortDirection(),
                query.filters()
        );

        assertThat(queryResult.results()).hasSize(1);
        assertThat(queryResult.results().get(0).getCurrentUrl()).isEqualTo("/home/features");
        assertThat(queryResult.totalCount()).isEqualTo(2);
    }

    static Stream<Arguments> queryCases() {
        return Stream.of(
                Arguments.of(
                        new AnalyticsQuery(
                                List.of(new AnalyticsQueryFilter("browser", "eq", "Chrome")),
                                "timestamp",
                                "asc",
                                1
                        ),
                        List.of(1L, 3L)
                ),
                Arguments.of(
                        new AnalyticsQuery(
                                List.of(new AnalyticsQueryFilter("currentUrl", "has", "home")),
                                "timestamp",
                                "desc",
                                1
                        ),
                        List.of(3L, 1L)
                ),
                Arguments.of(
                        new AnalyticsQuery(
                                List.of(
                                        new AnalyticsQueryFilter("timestamp", "gte", "2024-01-02 00:00:00"),
                                        new AnalyticsQueryFilter("timestamp", "lt", "2024-01-04 00:00:00")
                                ),
                                "timestamp",
                                "asc",
                                1
                        ),
                        List.of(2L, 3L)
                ),
                Arguments.of(
                        new AnalyticsQuery(
                                List.of(
                                        new AnalyticsQueryFilter("ipAddress", "has", "10.0.0"),
                                        new AnalyticsQueryFilter("browser", "neq", "Firefox")
                                ),
                                "currentUrl",
                                "asc",
                                1
                        ),
                        List.of(1L)
                )
        );
    }

    private Analytics analytics(
            AnalyticsEventTypes eventType,
            String currentUrl,
            String browser,
            String operatingSystem,
            String sessionId,
            String ipAddress,
            String timestamp
    ) {
        Analytics analytics = new Analytics();
        analytics.setEventType(eventType);
        analytics.setCurrentUrl(currentUrl);
        analytics.setBrowser(browser);
        analytics.setOperatingSystem(operatingSystem);
        analytics.setSessionId(sessionId);
        analytics.setIpAddress(ipAddress);
        analytics.setTimestamp(Instant.parse(timestamp));
        analytics.setPayload(Map.of("sample", true));
        return analytics;
    }
}
