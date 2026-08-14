package com.mowa.backend.service;

import com.mowa.backend.entity.Emotion;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ExperienceDraftAiClient {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_TAG_LENGTH = 50;
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter PROMPT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h시 m분", Locale.KOREAN);

    private final ChatClient chatClient;

    ExperienceDraftAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    ExperienceDraftAiGenerationResult generate(ExperienceDraftAiGenerationInput input) {
        AiGenerationModelResponse response = chatClient.prompt()
                .system(systemPrompt())
                .user(userPrompt(input))
                .options(OpenAiChatOptions.builder().maxRetries(0))
                .call()
                .entity(AiGenerationModelResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI response is empty.");
        }

        return validateAndNormalize(response);
    }

    private String systemPrompt() {
        return """
                You write concise Korean walk diary drafts for MOWA.
                Use only the user-provided draft fields and objective walk candidate fields.
                Do not invent relationships, conversations, events, places, names, memories, or emotions that are not provided.
                Do not infer places, emotions, people, or relationships from photo existence.
                If information is absent, omit it instead of filling it in.
                Do not output raw UTC or ISO-8601 timestamp strings in the diary body.
                Do not copy system timestamp syntax with date-time separators or UTC suffixes.
                If time is useful, express it naturally in Korean using the provided Korean local time.
                Return a title, body, and suggested tags. The title must be 100 characters or less.
                The body must be 2 to 4 concise Korean sentences.
                Suggested tags must be short Korean tags without '#', at most 10 items, each 50 characters or less.
                """;
    }

    private String userPrompt(ExperienceDraftAiGenerationInput input) {
        List<String> lines = new ArrayList<>();
        lines.add("Create an AI diary draft from this snapshot.");
        lines.add("Objective walk information:");
        lines.add("- 시작 시각: " + formatPromptDateTime(input.detectedStartAt()));
        addIfPresent(lines, "- 종료 시각: ", formatPromptDateTime(input.detectedEndAt()));
        addIfPresent(lines, "- durationSeconds: ", input.durationSeconds());
        addIfPresent(lines, "- locationSummary: ", input.locationSummary());

        lines.add("User-selected draft information:");
        lines.add("- hasPhoto: " + input.hasPhoto());
        addIfPresent(lines, "- companion: ", input.companion());
        if (!input.emotions().isEmpty()) {
            lines.add("- emotions: " + input.emotions().stream().map(Emotion::name).toList());
        }
        addIfPresent(lines, "- situation: ", input.situation());

        lines.add("hasPhoto only means a photo exists. Do not analyze photo content or infer facts from it.");
        return String.join(System.lineSeparator(), lines);
    }

    static String formatPromptDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZoneSameInstant(SERVICE_ZONE_ID).format(PROMPT_DATE_TIME_FORMATTER);
    }

    private void addIfPresent(List<String> lines, String label, Object value) {
        if (value instanceof String stringValue) {
            if (StringUtils.hasText(stringValue)) {
                lines.add(label + stringValue);
            }
            return;
        }

        if (value != null) {
            lines.add(label + value);
        }
    }

    private ExperienceDraftAiGenerationResult validateAndNormalize(AiGenerationModelResponse response) {
        String title = normalizeRequiredText(response.aiTitle(), "aiTitle");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalStateException("AI title is too long.");
        }

        String body = normalizeRequiredText(response.aiBody(), "aiBody");
        if (response.suggestedTags() == null) {
            throw new IllegalStateException("AI suggestedTags is missing.");
        }

        List<String> tags = normalizeTags(response.suggestedTags());
        return new ExperienceDraftAiGenerationResult(title, body, tags);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("AI " + fieldName + " is missing.");
        }
        return value.trim();
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags.size() > MAX_TAG_COUNT) {
            throw new IllegalStateException("AI suggestedTags has too many items.");
        }

        Set<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                throw new IllegalStateException("AI suggestedTags contains blank item.");
            }

            String normalizedTag = tag.trim();
            while (normalizedTag.startsWith("#")) {
                normalizedTag = normalizedTag.substring(1).trim();
            }

            if (!StringUtils.hasText(normalizedTag)) {
                throw new IllegalStateException("AI suggestedTags contains blank item.");
            }
            if (normalizedTag.length() > MAX_TAG_LENGTH) {
                throw new IllegalStateException("AI suggestedTags item is too long.");
            }
            normalizedTags.add(normalizedTag);
        }

        return List.copyOf(normalizedTags);
    }

    private record AiGenerationModelResponse(
            String aiTitle,
            String aiBody,
            List<String> suggestedTags
    ) {
    }
}
