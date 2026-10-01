package apiservice.service;

import apiservice.dbentity.ChatInteraction;
import apiservice.repository.ChatInteractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
}
