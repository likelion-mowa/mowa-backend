package com.mowa.backend.repository;

import com.mowa.backend.entity.ExperienceDraft;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExperienceDraftRepository extends JpaRepository<ExperienceDraft, UUID> {

    boolean existsByCandidate_Id(UUID candidateId);

    Optional<ExperienceDraft> findByIdAndUser_IdAndDemoSessionId(UUID id, UUID userId, UUID demoSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from ExperienceDraft d
            join fetch d.candidate
            where d.id = :id
              and d.user.id = :userId
              and d.demoSessionId = :demoSessionId
            """)
    Optional<ExperienceDraft> findByIdAndUser_IdAndDemoSessionIdForUpdate(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("demoSessionId") UUID demoSessionId
    );
}
