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

    Optional<ExperienceDraft> findByIdAndUser_Id(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from ExperienceDraft d
            join fetch d.candidate
            where d.id = :id
              and d.user.id = :userId
            """)
    Optional<ExperienceDraft> findByIdAndUser_IdForUpdate(
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );
}
