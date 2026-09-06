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
    void returnsRedisCachedAnswerWithoutQueryingTheDatabase() {
        ChatInteractionRepository repository = mock(ChatInteractionRepository.class);
        ChatAnswerCache cache = mock(ChatAnswerCache.class);
        when(cache.get("What is your Java experience?")).thenReturn("Cached answer");
        ChatServiceImpl service = new ChatServiceImpl(null, repository, new ObjectMapper(), cache);

        String answer = service.ask("What is your Java experience?");

        assertEquals("Cached answer", answer);
        verify(repository, never()).findFirstByQuestionIgnoreCaseOrderByCreatedAtDesc("What is your Java experience?");
    }

    @Test
    void returnsPersistedAnswerAndAddsItToRedisWithoutCallingOpenAi() {
        ChatInteractionRepository repository = mock(ChatInteractionRepository.class);
        ChatAnswerCache cache = mock(ChatAnswerCache.class);
        ChatInteraction interaction = new ChatInteraction();
        interaction.setAnswer("Stored answer");
        when(repository.findFirstByQuestionIgnoreCaseOrderByCreatedAtDesc("What is your Java experience?"))
                .thenReturn(interaction);
        ChatServiceImpl service = new ChatServiceImpl(null, repository, new ObjectMapper(), cache);

        String answer = service.ask("What is your Java experience?");

        assertEquals("Stored answer", answer);
        verify(cache).put("What is your Java experience?", "Stored answer");
    }
}
