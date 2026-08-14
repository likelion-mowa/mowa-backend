package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mowa.backend.dto.walkexperience.UpdateWalkExperienceRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class UpdateWalkExperienceRequestTest {

    private static ObjectMapper objectMapper;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void distinguishesOmittedFieldsFromExplicitNullAndArrays() throws Exception {
        UpdateWalkExperienceRequest omitted = read("{}");
        UpdateWalkExperienceRequest nullBody = read("{\"body\":null}");
        UpdateWalkExperienceRequest emptyEmotions = read("{\"emotions\":[]}");
        UpdateWalkExperienceRequest nullEmotions = read("{\"emotions\":null}");

        assertThat(omitted.hasTitle()).isFalse();
        assertThat(omitted.hasBody()).isFalse();
        assertThat(omitted.hasEmotions()).isFalse();
        assertThat(nullBody.hasBody()).isTrue();
        assertThat(nullBody.getBody()).isNull();
        assertThat(emptyEmotions.hasEmotions()).isTrue();
        assertThat(emptyEmotions.getEmotions()).isEmpty();
        assertThat(nullEmotions.hasEmotions()).isTrue();
        assertThat(nullEmotions.getEmotions()).isNull();
    }

    @Test
    void beanValidationCoversBasicTitleAndTagConstraints() throws Exception {
        assertThat(validator.validate(read("{\"title\":\"" + "a".repeat(101) + "\"}"))).isNotEmpty();
        assertThat(validator.validate(read("{\"tags\":[\"\"]}"))).isNotEmpty();
        assertThat(validator.validate(read("{\"tags\":[\"" + "a".repeat(51) + "\"]}"))).isNotEmpty();
        assertThat(validator.validate(read(
                "{\"tags\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\"]}"
        ))).isNotEmpty();
    }

    private UpdateWalkExperienceRequest read(String json) throws Exception {
        return objectMapper.readValue(json, UpdateWalkExperienceRequest.class);
    }
}
