package apiservice.repository;

import apiservice.dbentity.ChatInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {
}
