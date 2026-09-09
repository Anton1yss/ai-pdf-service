package by.AntonDemchuk.ai_pdf_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final OpenAIClient client;

    public List<String> getPhrasesToRedact(String pdfText, String userPrompt) {

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .addSystemMessage("""
                    You are a document redaction assistant.
                    Return ONLY a valid JSON array of exact phrases to redact.
                    No explanation, no markdown, no extra text.
                    Example: ["Anton D", "+48 999 999 999"]
                    If nothing matches return: []
                    """)
                .addUserMessage(String.format("""
                    Redaction request: "%s"
                    Document text: %s
                    """, userPrompt, pdfText))
                .build();

        String response = client.chat()
                .completions()
                .create(params)
                .choices()
                .get(0)
                .message()
                .content()
                .orElseThrow(() -> new RuntimeException("Empty response"));

        try {
            return new ObjectMapper().readValue(response, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + response, e);
        }
    }

}