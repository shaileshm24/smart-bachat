package com.ametsa.smartbachat.uam.repository;

import com.ametsa.smartbachat.uam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    List<Permission> findByResource(String resource);

    List<Permission> findByAction(String action);

    boolean existsByName(String name);
}

