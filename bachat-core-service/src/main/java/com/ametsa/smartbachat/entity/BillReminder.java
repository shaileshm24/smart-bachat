package com.ametsa.smartbachat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a recurring bill reminder.
 * Users can set reminders for rent, utilities, subscriptions, etc.
 */
@Entity
@Table(name = "bill_reminders", indexes = {
        @Index(name = "idx_bill_reminder_profile", columnList = "profile_id"),
        @Index(name = "idx_bill_reminder_due_date", columnList = "next_due_date"),
        @Index(name = "idx_bill_reminder_active", columnList = "is_active")
})
public class BillReminder {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "name", nullable = false)
    private String name;

    // Category: RENT, UTILITIES, SUBSCRIPTION, INSURANCE, EMI, CREDIT_CARD, OTHER
    @Column(name = "category")
    private String category;

    // Amount in paisa
    @Column(name = "amount")
    private Long amount;

    // Frequency: WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
    @Column(name = "frequency", nullable = false)
    private String frequency;

    // For CUSTOM frequency: number of days between reminders
    @Column(name = "custom_days")
    private Integer customDays;

    // Next due date
    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    // Days before due date to send reminder
    @Column(name = "reminder_days_before")
    private Integer reminderDaysBefore;

    // Is this reminder active?
    @Column(name = "is_active")
    private Boolean isActive;

    // Auto-pay enabled?
    @Column(name = "is_auto_pay")
    private Boolean isAutoPay;

    // Payee/Merchant name
    @Column(name = "payee")
    private String payee;

    // Account/Reference number
    @Column(name = "account_reference")
    private String accountReference;

    // Icon identifier for UI
    @Column(name = "icon")
    private String icon;

    // Color code for UI
    @Column(name = "color")
    private String color;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // Last paid date
    @Column(name = "last_paid_date")
    private LocalDate lastPaidDate;

    // Last paid amount in paisa
    @Column(name = "last_paid_amount")
    private Long lastPaidAmount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public BillReminder() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.isActive = true;
        this.isAutoPay = false;
        this.reminderDaysBefore = 3;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getCustomDays() { return customDays; }
    public void setCustomDays(Integer customDays) { this.customDays = customDays; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public Integer getReminderDaysBefore() { return reminderDaysBefore; }
    public void setReminderDaysBefore(Integer reminderDaysBefore) { this.reminderDaysBefore = reminderDaysBefore; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsAutoPay() { return isAutoPay; }
    public void setIsAutoPay(Boolean isAutoPay) { this.isAutoPay = isAutoPay; }

    public String getPayee() { return payee; }
    public void setPayee(String payee) { this.payee = payee; }

    public String getAccountReference() { return accountReference; }
    public void setAccountReference(String accountReference) { this.accountReference = accountReference; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getLastPaidDate() { return lastPaidDate; }
    public void setLastPaidDate(LocalDate lastPaidDate) { this.lastPaidDate = lastPaidDate; }

    public Long getLastPaidAmount() { return lastPaidAmount; }
    public void setLastPaidAmount(Long lastPaidAmount) { this.lastPaidAmount = lastPaidAmount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

