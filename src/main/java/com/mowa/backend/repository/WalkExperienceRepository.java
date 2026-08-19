package com.mowa.backend.repository;

import com.mowa.backend.entity.WalkExperience;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalkExperienceRepository extends JpaRepository<WalkExperience, UUID> {

    boolean existsByDraft_Id(UUID draftId);

    boolean existsByUser_IdAndDemoSessionId(UUID userId, UUID demoSessionId);

    @Query("""
            select distinct e
            from WalkExperience e
            join fetch e.draft d
            join fetch d.candidate
            left join fetch d.emotions
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.demoSessionId = :demoSessionId
              and e.deletedAt is null
            order by e.startedAt asc
            """)
    List<WalkExperience> findAllActiveTemplatesByUserIdAndDemoSessionId(
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId
    );

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.demoSessionId = :demoSessionId
              and e.deletedAt is null
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserIdAndDemoSessionId(
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId
    );

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.demoSessionId = :demoSessionId
              and e.deletedAt is null
              and e.startedAt >= :startInclusive
              and e.startedAt < :endExclusive
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserIdAndDemoSessionIdAndStartedAtRange(
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId,
            @Param("startInclusive") OffsetDateTime startInclusive,
            @Param("endExclusive") OffsetDateTime endExclusive
    );

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.demoSessionId = :demoSessionId
              and e.deletedAt is null
              and :tag member of e.tags
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserIdAndDemoSessionIdAndTag(
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId,
            @Param("tag") String tag
    );

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.id = :experienceId
              and e.user.id = :userId
              and e.demoSessionId = :demoSessionId
              and e.deletedAt is null
            """)
    Optional<WalkExperience> findActiveByIdAndUserIdAndDemoSessionId(
            @Param("experienceId") UUID experienceId,
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId
    );
}
