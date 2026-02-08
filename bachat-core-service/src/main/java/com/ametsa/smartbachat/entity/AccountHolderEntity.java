package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores account holder profile information from Setu AA.
 * This data comes from the profile.holders section of the Setu response.
 */
@Entity
@Table(name = "account_holders", indexes = {
        @Index(name = "idx_holder_user", columnList = "user_id"),
        @Index(name = "idx_holder_bank_account", columnList = "bank_account_id"),
        @Index(name = "idx_holder_pan", columnList = "pan")
})
public class AccountHolderEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    // User ID from UAM service (logged-in user)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Reference to the bank account
    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    // Holder type: SINGLE, JOINT
    @Column(name = "holder_type")
    private String holderType;

    // Holder details from Setu
    @Column(name = "name")
    private String name;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email")
    private String email;

    @Column(name = "pan")
    private String pan;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "nominee")
    private String nominee;

    @Column(name = "ckyc_compliance")
    private String ckycCompliance;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public AccountHolderEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(UUID bankAccountId) { this.bankAccountId = bankAccountId; }
    public String getHolderType() { return holderType; }
    public void setHolderType(String holderType) { this.holderType = holderType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNominee() { return nominee; }
    public void setNominee(String nominee) { this.nominee = nominee; }
    public String getCkycCompliance() { return ckycCompliance; }
    public void setCkycCompliance(String ckycCompliance) { this.ckycCompliance = ckycCompliance; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

