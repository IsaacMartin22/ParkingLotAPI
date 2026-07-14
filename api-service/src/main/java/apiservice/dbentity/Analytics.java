package apiservice.dbentity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import parkinglot.common.model.AnalyticsEventTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "analytics")
@Getter
@Setter
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private AnalyticsEventTypes eventType;

    @Column(name = "current_url")
    private String currentUrl;

    private String browser;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "session_id")
    private String sessionId;

    private Instant timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;
}
