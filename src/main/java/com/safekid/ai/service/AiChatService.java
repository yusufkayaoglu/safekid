package com.safekid.ai.service;

import com.safekid.ai.client.ClaudeApiClient;
import com.safekid.ai.dto.AiChatResponse;
import com.safekid.ai.entity.AiChatSessionEntity;
import com.safekid.ai.entity.ChatRole;
import com.safekid.ai.repository.AiChatSessionRepository;
import com.safekid.child.entity.CocukKonumEntity;
import com.safekid.child.repository.CocukKonumRepository;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ClaudeApiClient claudeApiClient;
    private final CocukKonumRepository konumRepository;
    private final ChildRepository childRepository;
    private final AiChatSessionRepository chatSessionRepository;
    private final LocationDataCollector dataCollector;

    private static final String SYSTEM_PROMPT = """
            Sen SafeKid çocuk takip uygulamasının AI asistanısın.
            Ebeveynlere çocuklarının konum bilgileri hakkında Türkçe olarak yardımcı olursun.

            Görevlerin:
            - Ebeveynin sorularını doğal dilde yanıtla
            - Konum verilerini analiz et ve anlaşılır şekilde açıkla
            - Güvenlik önerileri sun
            - Samimi ama profesyonel bir dil kullan

            Sana verilen konum verileri en güncel verilerdir. Bunları kullanarak cevap ver.
            Her zaman Türkçe yanıt ver.
            """;

    public AiChatResponse chat(String parentId, String cocukUniqueId, String userMessage) {
        ChildEntity child = childRepository
                .findByCocukUniqueIdAndParent_EbeveynUniqueId(cocukUniqueId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Çocuk bulunamadı"));

        // Get recent location data for context
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        List<CocukKonumEntity> recentLocations =
                konumRepository.findByChild_CocukUniqueIdAndRecordedAtAfterOrderByRecordedAtAsc(
                        cocukUniqueId, oneDayAgo);

        String locationContext = recentLocations.isEmpty()
                ? "Son 24 saatte konum verisi yok."
                : dataCollector.formatLocationsForPrompt(recentLocations);

        // Get chat history (last 20 messages, reversed to chronological order)
        List<AiChatSessionEntity> history =
                chatSessionRepository.findTop20ByParentIdAndChild_CocukUniqueIdOrderByCreatedAtDesc(
                        parentId, cocukUniqueId);
        List<AiChatSessionEntity> chronological = new ArrayList<>(history.reversed());

        // Build messages list
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add context as first user message if there's location data
        if (!recentLocations.isEmpty()) {
            messages.add(Map.of("role", "user", "content",
                    "[Sistem Bilgisi - Çocuk: " + child.getCocukAdi() + " " + child.getCocukSoyadi()
                    + "]\n\nSon 24 saat konum verileri:\n" + locationContext));
            messages.add(Map.of("role", "assistant", "content",
                    "Anladım, " + child.getCocukAdi() + "'in konum verilerini inceliyorum. Size nasıl yardımcı olabilirim?"));
        }

        // Add chat history
        for (AiChatSessionEntity msg : chronological) {
            String role = msg.getRole() == ChatRole.USER ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        // Add current message
        messages.add(Map.of("role", "user", "content", userMessage));

        String aiResponse = claudeApiClient.sendMessages(SYSTEM_PROMPT, messages);

        // Save user message
        AiChatSessionEntity userMsg = new AiChatSessionEntity();
        userMsg.setChild(child);
        userMsg.setParentId(parentId);
        userMsg.setRole(ChatRole.USER);
        userMsg.setContent(userMessage);
        chatSessionRepository.save(userMsg);

        // Save assistant response
        AiChatSessionEntity assistantMsg = new AiChatSessionEntity();
        assistantMsg.setChild(child);
        assistantMsg.setParentId(parentId);
        assistantMsg.setRole(ChatRole.ASSISTANT);
        assistantMsg.setContent(aiResponse);
        chatSessionRepository.save(assistantMsg);

        return new AiChatResponse(cocukUniqueId, aiResponse);
    }
}
