package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.BudgetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, UUID> {

    /**
     * Find all active templates.
     */
    List<BudgetTemplate> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find all system templates.
     */
    List<BudgetTemplate> findByIsSystemTrueAndIsActiveTrueOrderByNameAsc();

    /**
     * Find templates by type.
     */
    List<BudgetTemplate> findByTemplateTypeAndIsActiveTrueOrderByNameAsc(String templateType);
}

