package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.controller.WalkExperienceController;
import com.mowa.backend.security.AuthenticatedUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class WalkExperienceDeleteControllerTest {

    @Test
    void returnsOkApiResponseAndPassesAuthenticatedIdsToService() {
        WalkExperienceService service = mock(WalkExperienceService.class);
        WalkExperienceController controller = new WalkExperienceController(service);
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        UUID experienceId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> response = controller.delete(
                new AuthenticatedUser(userId, demoSessionId),
                experienceId
        );

        verify(service).delete(userId, demoSessionId, experienceId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isNotBlank();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getError()).isNull();
    }
}
