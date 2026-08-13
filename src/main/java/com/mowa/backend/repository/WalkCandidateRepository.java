package com.mowa.backend.repository;

import com.mowa.backend.entity.WalkCandidate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalkCandidateRepository extends JpaRepository<WalkCandidate, UUID> {
}
