package com.mowa.backend.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DemoSessionEntityMappingTest {

    @Test
    void walkFlowEntitiesMapDemoSessionIdAsRequiredColumn() throws NoSuchFieldException {
        assertRequiredDemoSessionColumn(WalkCandidate.class);
        assertRequiredDemoSessionColumn(ExperienceDraft.class);
        assertRequiredDemoSessionColumn(WalkExperience.class);
    }

    @Test
    void walkExperienceDateLookupIndexIncludesDemoSession() {
        Table table = WalkExperience.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.indexes()).map(Index::name))
                .contains("idx_walk_experiences_user_demo_session_started_at");
        assertThat(Arrays.stream(table.indexes()).map(Index::columnList))
                .contains("user_id, demo_session_id, started_at");
    }

    private void assertRequiredDemoSessionColumn(Class<?> entityType) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField("demoSessionId");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("demo_session_id");
        assertThat(column.nullable()).isFalse();
    }
}
