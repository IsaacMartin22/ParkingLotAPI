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

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO chat_interactions (question, answer, embedding, embedding_model, chat_model)
            VALUES (:question, :answer, CAST(:embedding AS vector), :embeddingModel, :chatModel)
            """, nativeQuery = true)
    void insertWithVectorCast(
            @Param("question") String question,
            @Param("answer") String answer,
            @Param("embedding") String embedding,
            @Param("embeddingModel") String embeddingModel,
            @Param("chatModel") String chatModel
    );
}
