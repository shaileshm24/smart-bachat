package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.StatementMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatementMetadataRepository extends JpaRepository<StatementMetadata, UUID> {
}
