package com.ametsa.smartbachat.uam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Profile entity linked to User.
 * This is the profile_id used in transactions and bank accounts.
 */
@Entity
@Table(name = "profiles", indexes = {
        @Index(name = "idx_profiles_user", columnList = "user_id")
})
public class Profile {

    @Id
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "aadhaar_last_four")
    private String aadhaarLastFour;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "country")
    private String country;

    // Preferences
    @Column(name = "preferred_currency")
    private String preferredCurrency;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "language")
    private String language;

    // Notification preferences
    @Column(name = "email_notifications")
    private Boolean emailNotifications;

    @Column(name = "sms_notifications")
    private Boolean smsNotifications;

    @Column(name = "push_notifications")
    private Boolean pushNotifications;

    // Financial profile
    @Column(name = "monthly_income_range")
    private String monthlyIncomeRange;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Profile() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.preferredCurrency = "INR";
        this.timezone = "Asia/Kolkata";
        this.language = "en";
        this.country = "India";
        this.emailNotifications = true;
        this.smsNotifications = true;
        this.pushNotifications = true;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getAadhaarLastFour() { return aadhaarLastFour; }
    public void setAadhaarLastFour(String aadhaarLastFour) { this.aadhaarLastFour = aadhaarLastFour; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPreferredCurrency() { return preferredCurrency; }
    public void setPreferredCurrency(String preferredCurrency) { this.preferredCurrency = preferredCurrency; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public Boolean getSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(Boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    public String getMonthlyIncomeRange() { return monthlyIncomeRange; }
    public void setMonthlyIncomeRange(String monthlyIncomeRange) { this.monthlyIncomeRange = monthlyIncomeRange; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

