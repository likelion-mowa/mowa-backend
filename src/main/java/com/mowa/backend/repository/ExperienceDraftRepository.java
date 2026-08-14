package com.mowa.backend.repository;

import com.mowa.backend.entity.ExperienceDraft;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceDraftRepository extends JpaRepository<ExperienceDraft, UUID> {

    boolean existsByCandidate_Id(UUID candidateId);

    Optional<ExperienceDraft> findByIdAndUser_Id(UUID id, UUID userId);
}
