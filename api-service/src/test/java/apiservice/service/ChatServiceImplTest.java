package apiservice.service;

import apiservice.dbentity.ChatInteraction;
import apiservice.model.PortfolioDocument;
import apiservice.repository.ChatInteractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    @Test
    void returnsPersistedAnswerWithoutUsingRedisCache() {
        ChatInteractionRepository repository = mock(ChatInteractionRepository.class);
        ChatInteraction interaction = new ChatInteraction();
        interaction.setAnswer("Stored answer");
        when(repository.findFirstByQuestionIgnoreCaseOrderByCreatedAtDesc("What is your Java experience?"))
                .thenReturn(interaction);
        ChatServiceImpl service = new ChatServiceImpl(null, repository, new ObjectMapper());

        String answer = service.ask("What is your Java experience?");

        assertEquals("Stored answer", answer);
    }

    @Test
    void choosesMostFrequentCitationAcrossRetrievedDocuments() {
        List<PortfolioDocument> documents = List.of(
                document("https://example.com/resume", 0.55),
                document("https://example.com/github", 0.92),
                document("https://example.com/resume", 0.80),
                document("https://example.com/resume", 0.61),
                document("https://example.com/portfolio", 0.96)
        );

        assertEquals("https://example.com/resume", ChatServiceImpl.chooseCitation(documents));
    }

    private PortfolioDocument document(String citation, double score) {
        PortfolioDocument portfolioDocument = new PortfolioDocument();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("citation", citation);
        portfolioDocument.setMetadata(metadata);
        portfolioDocument.setScore(score);
        return portfolioDocument;
    }
}
