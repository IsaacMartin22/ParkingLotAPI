package apiservice.service;

import apiservice.model.PortfolioDocument;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatServiceImplTest {

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
