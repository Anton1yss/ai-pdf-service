package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.service.AIService;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AIServiceTest extends BaseServiceTest {

    private static final String PDF_TEXT = "John Smith lives at 5 Main Street";
    private static final String USER_PROMPT = "hide all personal data";

    @InjectMocks
    private AIService aiService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient client;

    @Test
    public void getPhrasesToRedact_shouldReturnPhrases() {

        /* Arranging */
        mockResponse(Optional.of("[\"John Smith\", \"5 Main Street\"]"));

        /* Acting */
        List<String> res = aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT);

        /* Asserting & Verifying */
        assertEquals(List.of("John Smith", "5 Main Street"), res);
        verify(client.chat().completions()).create(any(ChatCompletionCreateParams.class));
    }

    @Test
    public void getPhrasesToRedact_shouldReturnEmptyList_whenNothingMatches() {

        /* Arranging */
        mockResponse(Optional.of("[]"));

        /* Acting */
        List<String> res = aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT);

        /* Asserting */
        assertTrue(res.isEmpty());
    }

    @Test
    public void getPhrasesToRedact_shouldSendPromptAndDocumentToModel() {

        /* Arranging */
        mockResponse(Optional.of("[]"));

        /* Acting */
        aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT);

        /* Asserting & Verifying */
        ChatCompletionCreateParams params = captureParams();
        String messages = params.messages().toString();
        assertTrue(messages.contains(USER_PROMPT));
        assertTrue(messages.contains(PDF_TEXT));
        assertTrue(messages.contains("<document>"));
    }

    @Test
    public void getPhrasesToRedact_shouldThrow_whenResponseIsNotJson() {

        /* Arranging */
        mockResponse(Optional.of("Sure! Here are the phrases: John Smith"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT));
        assertTrue(ex.getMessage().startsWith("Failed to parse OpenAI response"));
    }

    @Test
    public void getPhrasesToRedact_shouldThrow_whenResponseIsNotAnArray() {

        /* Arranging */
        mockResponse(Optional.of("{\"phrase\": \"John Smith\"}"));

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT));
        assertTrue(ex.getMessage().startsWith("Failed to parse OpenAI response"));
    }

    @Test
    public void getPhrasesToRedact_shouldThrow_whenResponseIsEmpty() {

        /* Arranging */
        mockResponse(Optional.empty());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aiService.getPhrasesToRedact(PDF_TEXT, USER_PROMPT));
        assertEquals("Empty response", ex.getMessage());
    }

    @Test
    public void summarize_shouldReturnSummary() {

        /* Arranging */
        mockResponse(Optional.of("Short summary"));

        /* Acting */
        String res = aiService.summarize(PDF_TEXT, "summarize in one line");

        /* Asserting & Verifying */
        assertEquals("Short summary", res);
        verify(client.chat().completions()).create(any(ChatCompletionCreateParams.class));
    }

    @Test
    public void summarize_shouldSendPromptAndDocumentToModel() {

        /* Arranging */
        mockResponse(Optional.of("Short summary"));

        /* Acting */
        aiService.summarize(PDF_TEXT, "summarize in one line");

        /* Asserting & Verifying */
        ChatCompletionCreateParams params = captureParams();
        String messages = params.messages().toString();
        assertTrue(messages.contains("summarize in one line"));
        assertTrue(messages.contains(PDF_TEXT));
        assertTrue(messages.contains("<document>"));
    }

    @Test
    public void summarize_shouldThrow_whenResponseIsEmpty() {

        /* Arranging */
        mockResponse(Optional.empty());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aiService.summarize(PDF_TEXT, "summarize"));
        assertEquals("Empty response", ex.getMessage());
    }

    private void mockResponse(Optional<String> content) {
        when(client.chat()
                .completions()
                .create(any(ChatCompletionCreateParams.class))
                .choices()
                .get(0)
                .message()
                .content())
                .thenReturn(content);
    }

    private ChatCompletionCreateParams captureParams() {
        ArgumentCaptor<ChatCompletionCreateParams> captor = ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
        verify(client.chat().completions(), atLeastOnce()).create(captor.capture());

        return captor.getAllValues().stream()
                .filter(java.util.Objects::nonNull)
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}