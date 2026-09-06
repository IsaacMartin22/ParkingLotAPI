package apiservice.repository;

import apiservice.dbentity.ChatInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {

    List<ChatInteraction> findByOrderByCreatedAtDesc(Pageable pageable);

    ChatInteraction findFirstByQuestionIgnoreCaseOrderByCreatedAtDesc(String question);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO chat_interactions (
                question,
                answer,
                embedding,
                embedding_model,
                chat_model,
                cache_hit,
                embedding_latency_ms,
                vector_search_duration_ms,
                vector_search_document_count
            )
            VALUES (
                :question,
                :answer,
                CAST(:embedding AS vector),
                :embeddingModel,
                :chatModel,
                :cacheHit,
                :embeddingLatencyMs,
                :vectorSearchDurationMs,
                :vectorSearchDocumentCount
            )
            """, nativeQuery = true)
    void insertWithVectorCast(
            @Param("question") String question,
            @Param("answer") String answer,
            @Param("embedding") String embedding,
            @Param("embeddingModel") String embeddingModel,
            @Param("chatModel") String chatModel,
            @Param("cacheHit") boolean cacheHit,
            @Param("embeddingLatencyMs") long embeddingLatencyMs,
            @Param("vectorSearchDurationMs") long vectorSearchDurationMs,
            @Param("vectorSearchDocumentCount") int vectorSearchDocumentCount
    );
}
