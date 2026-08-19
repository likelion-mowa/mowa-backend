package com.mowa.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class WalkExperienceRepositoryQueryTest {

    @Test
    void allListQueriesApplyOwnershipSoftDeleteFetchAndDescendingOrder() {
        assertCommonConditions(query("findAllActiveByUserIdAndDemoSessionId", UUID.class, UUID.class));
        assertCommonConditions(query(
                "findAllActiveByUserIdAndDemoSessionIdAndStartedAtRange",
                UUID.class,
                UUID.class,
                OffsetDateTime.class,
                OffsetDateTime.class
        ));
        assertCommonConditions(query("findAllActiveByUserIdAndDemoSessionIdAndTag", UUID.class, UUID.class, String.class));
    }

    @Test
    void dateRangeQueryUsesInclusiveStartAndExclusiveEnd() {
        String query = query(
                "findAllActiveByUserIdAndDemoSessionIdAndStartedAtRange",
                UUID.class,
                UUID.class,
                OffsetDateTime.class,
                OffsetDateTime.class
        );

        assertThat(query).contains(
                "e.startedAt >= :startInclusive",
                "e.startedAt < :endExclusive"
        );
    }

    @Test
    void tagQueryUsesMembershipWithoutRestrictingFetchedTags() {
        String query = query("findAllActiveByUserIdAndDemoSessionIdAndTag", UUID.class, UUID.class, String.class);

        assertThat(query).contains("left join fetch e.tags", ":tag member of e.tags");
        assertThat(query).doesNotContain("where tag = :tag", "join fetch e.tags tag");
    }

    @Test
    void detailQueryAppliesIdOwnershipSoftDeleteAndFetchesCollectionsOnly() {
        String query = query("findActiveByIdAndUserIdAndDemoSessionId", UUID.class, UUID.class, UUID.class);

        assertThat(query).contains(
                "select distinct e",
                "left join fetch e.emotions",
                "left join fetch e.tags",
                "e.id = :experienceId",
                "e.user.id = :userId",
                "e.demoSessionId = :demoSessionId",
                "e.deletedAt is null"
        );
        assertThat(query).doesNotContain(
                "e.draft",
                "e.candidate",
                "order by e.startedAt"
        );
    }

    private void assertCommonConditions(String query) {
        assertThat(query).contains(
                "select distinct e",
                "left join fetch e.emotions",
                "left join fetch e.tags",
                "e.user.id = :userId",
                "e.demoSessionId = :demoSessionId",
                "e.deletedAt is null",
                "order by e.startedAt desc"
        );
        assertThat(query).doesNotContain("e.draft", "e.candidate");
    }

    private String query(String methodName, Class<?>... parameterTypes) {
        try {
            Method method = WalkExperienceRepository.class.getMethod(methodName, parameterTypes);
            return method.getAnnotation(Query.class).value();
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void templateQueryFetchesDraftCandidateCollectionsAndOnlyActiveTemplateExperiences() {
        String query = query("findAllActiveTemplatesByUserIdAndDemoSessionId", UUID.class, UUID.class);

        assertThat(query).contains(
                "select distinct e",
                "join fetch e.draft d",
                "join fetch d.candidate",
                "left join fetch d.emotions",
                "left join fetch e.emotions",
                "left join fetch e.tags",
                "e.user.id = :userId",
                "e.demoSessionId = :demoSessionId",
                "e.deletedAt is null",
                "order by e.startedAt asc"
        );
    }
}
