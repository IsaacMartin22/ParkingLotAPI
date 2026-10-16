package apiservice.dbentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "chat_tool_usages")
@Getter
@Setter
public class ChatToolUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_interaction_id", nullable = false)
    private ChatInteraction chatInteraction;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "call_id")
    private String callId;

    @Column(name = "tool_round", nullable = false)
    private Integer toolRound;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "is_error", nullable = false)
    private Boolean error;

    @Column(name = "error_diagnostic")
    private String errorDiagnostic;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
