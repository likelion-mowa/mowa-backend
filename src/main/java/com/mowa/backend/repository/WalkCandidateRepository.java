package com.mowa.backend.repository;

import com.mowa.backend.entity.WalkCandidate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalkCandidateRepository extends JpaRepository<WalkCandidate, UUID> {

    Optional<WalkCandidate> findByIdAndUser_Id(UUID id, UUID userId);
}
