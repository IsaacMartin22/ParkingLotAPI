package apiservice.dbentity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_interactions")
@Getter
@Setter
public class ChatInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String answer;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(3072)")
    @ColumnTransformer(write = "CAST(? AS vector)")
    private String embedding;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "chat_model", nullable = false)
    private String chatModel;

    @Column(name = "cache_hit")
    private Boolean cacheHit;

    @Column(name = "embedding_latency_ms")
    private Long embeddingLatencyMs;

    @Column(name = "vector_search_duration_ms")
    private Long vectorSearchDurationMs;

    @Column(name = "vector_search_document_count")
    private Integer vectorSearchDocumentCount;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "chatInteraction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatToolUsage> toolUsages = new ArrayList<>();

    public void addToolUsage(ChatToolUsage toolUsage) {
        toolUsage.setChatInteraction(this);
        toolUsages.add(toolUsage);
    }
}
