package com.ametsa.smartbachat.controller;

import com.ametsa.smartbachat.dto.CategoryMappingDto;
import com.ametsa.smartbachat.dto.CreateCategoryMappingRequest;
import com.ametsa.smartbachat.dto.UncategorizedPatternDto;
import com.ametsa.smartbachat.entity.PatternType;
import com.ametsa.smartbachat.entity.UserCategoryMapping;
import com.ametsa.smartbachat.repository.UserCategoryMappingRepository;
import com.ametsa.smartbachat.security.SecurityUtils;
import com.ametsa.smartbachat.service.RecurringPatternDetectionService;
import com.ametsa.smartbachat.service.TransactionCategorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing user-defined category mappings.
 * Allows users to create custom categorization rules for their transactions.
 */
@RestController
@RequestMapping("/api/category-mappings")
public class CategoryMappingController {

    private static final Logger log = LoggerFactory.getLogger(CategoryMappingController.class);

    private final UserCategoryMappingRepository mappingRepository;
    private final RecurringPatternDetectionService patternDetectionService;
    private final TransactionCategorizationService categorizationService;
    private final SecurityUtils securityUtils;

    public CategoryMappingController(
            UserCategoryMappingRepository mappingRepository,
            RecurringPatternDetectionService patternDetectionService,
            TransactionCategorizationService categorizationService,
            SecurityUtils securityUtils) {
        this.mappingRepository = mappingRepository;
        this.patternDetectionService = patternDetectionService;
        this.categorizationService = categorizationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all category mappings for the current user.
     */
    @GetMapping
    public ResponseEntity<List<CategoryMappingDto>> getMappings() {
        UUID profileId = securityUtils.requireCurrentProfileId();
        List<UserCategoryMapping> mappings = mappingRepository.findByProfileIdOrderByMatchCountDesc(profileId);
        return ResponseEntity.ok(mappings.stream().map(this::toDto).collect(Collectors.toList()));
    }

    /**
     * Create a new category mapping.
     */
    @PostMapping
    public ResponseEntity<CategoryMappingDto> createMapping(@Valid @RequestBody CreateCategoryMappingRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        // Check if mapping already exists
        if (mappingRepository.existsByProfileIdAndPatternTypeAndPatternValue(
                profileId, request.getPatternType(), request.getPatternValue())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserCategoryMapping mapping = new UserCategoryMapping();
        mapping.setProfileId(profileId);
        mapping.setPatternType(request.getPatternType());
        mapping.setPatternValue(request.getPatternValue());
        mapping.setDisplayName(request.getDisplayName());
        mapping.setCategory(request.getCategory().toUpperCase());
        mapping.setSubCategory(request.getSubCategory());
        mapping.setIsRecurring(request.getIsRecurring());

        mapping = mappingRepository.save(mapping);
        log.info("Created category mapping {} for profile {}", mapping.getId(), profileId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(mapping));
    }

    /**
     * Update an existing category mapping.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryMappingDto> updateMapping(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryMappingRequest request) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        return mappingRepository.findById(id)
                .filter(m -> m.getProfileId().equals(profileId))
                .map(mapping -> {
                    mapping.setPatternType(request.getPatternType());
                    mapping.setPatternValue(request.getPatternValue());
                    mapping.setDisplayName(request.getDisplayName());
                    mapping.setCategory(request.getCategory().toUpperCase());
                    mapping.setSubCategory(request.getSubCategory());
                    mapping.setIsRecurring(request.getIsRecurring());
                    mapping.setUpdatedAt(Instant.now());
                    mapping = mappingRepository.save(mapping);
                    return ResponseEntity.ok(toDto(mapping));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a category mapping.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable UUID id) {
        UUID profileId = securityUtils.requireCurrentProfileId();

        return mappingRepository.findById(id)
                .filter(m -> m.getProfileId().equals(profileId))
                .map(mapping -> {
                    mappingRepository.delete(mapping);
                    log.info("Deleted category mapping {} for profile {}", id, profileId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get suggestions for uncategorized recurring patterns.
     * These are patterns detected from transactions that the user can map.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<UncategorizedPatternDto>> getSuggestions(
            @RequestParam(defaultValue = "2") int minOccurrences) {
        UUID profileId = securityUtils.requireCurrentProfileId();
        List<UncategorizedPatternDto> suggestions = 
                patternDetectionService.detectUncategorizedPatterns(profileId, minOccurrences);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get available categories.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(List.of(
                "FOOD", "GROCERIES", "TRANSPORT", "UTILITIES", "ENTERTAINMENT",
                "SHOPPING", "HEALTH", "EDUCATION", "INVESTMENT", "TRANSFER",
                "SALARY", "ATM", "EMI", "INSURANCE", "RENT", "SUBSCRIPTION",
                "FAMILY", "FRIENDS", "PERSONAL", "OTHER"
        ));
    }

    private CategoryMappingDto toDto(UserCategoryMapping entity) {
        CategoryMappingDto dto = new CategoryMappingDto();
        dto.setId(entity.getId());
        dto.setPatternType(entity.getPatternType());
        dto.setPatternValue(entity.getPatternValue());
        dto.setDisplayName(entity.getDisplayName());
        dto.setCategory(entity.getCategory());
        dto.setSubCategory(entity.getSubCategory());
        dto.setIsRecurring(entity.getIsRecurring());
        dto.setMatchCount(entity.getMatchCount());
        dto.setLastMatchedAt(entity.getLastMatchedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

