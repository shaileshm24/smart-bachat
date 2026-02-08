package com.ametsa.smartbachat.repository;

import com.ametsa.smartbachat.entity.AccountHolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolderEntity, UUID> {
    
    List<AccountHolderEntity> findByUserId(UUID userId);
    
    List<AccountHolderEntity> findByBankAccountId(UUID bankAccountId);
    
    List<AccountHolderEntity> findByUserIdAndBankAccountId(UUID userId, UUID bankAccountId);
    
    void deleteByBankAccountId(UUID bankAccountId);
}

