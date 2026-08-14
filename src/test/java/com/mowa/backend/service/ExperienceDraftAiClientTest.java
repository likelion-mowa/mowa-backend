package com.mowa.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ExperienceDraftAiClientTest {

    @Test
    void formatPromptDateTimeConvertsToKoreanLocalTime() {
        OffsetDateTime utcDateTime = OffsetDateTime.parse("2026-08-14T10:30:00Z");

        String formatted = ExperienceDraftAiClient.formatPromptDateTime(utcDateTime);

        assertEquals("2026년 8월 14일 오후 7시 30분", formatted);
    }

    @Test
    void formatPromptDateTimeReturnsNullWhenValueIsAbsent() {
        assertNull(ExperienceDraftAiClient.formatPromptDateTime(null));
    }
}
