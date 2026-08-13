package com.mowa.backend.repository;

import com.mowa.backend.entity.ExperienceDraft;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceDraftRepository extends JpaRepository<ExperienceDraft, UUID> {
}
