package com.mowa.backend.repository;

import com.mowa.backend.entity.WalkExperience;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalkExperienceRepository extends JpaRepository<WalkExperience, UUID> {

    boolean existsByDraft_Id(UUID draftId);
}
