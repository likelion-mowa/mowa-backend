package com.mowa.backend.repository;

import com.mowa.backend.entity.WalkExperience;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalkExperienceRepository extends JpaRepository<WalkExperience, UUID> {

    boolean existsByDraft_Id(UUID draftId);

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.deletedAt is null
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.deletedAt is null
              and e.startedAt >= :startInclusive
              and e.startedAt < :endExclusive
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserIdAndStartedAtRange(
            @Param("userId") UUID userId,
            @Param("startInclusive") OffsetDateTime startInclusive,
            @Param("endExclusive") OffsetDateTime endExclusive
    );

    @Query("""
            select distinct e
            from WalkExperience e
            left join fetch e.emotions
            left join fetch e.tags
            where e.user.id = :userId
              and e.deletedAt is null
              and :tag member of e.tags
            order by e.startedAt desc
            """)
    List<WalkExperience> findAllActiveByUserIdAndTag(
            @Param("userId") UUID userId,
            @Param("tag") String tag
    );
}
