package com.ametsa.smartbachat.entity;

/**
 * Types of patterns for user category mappings.
 */
public enum PatternType {
    /**
     * Match by counterparty name (the person/entity you paid or received from).
     * Uses case-insensitive contains matching.
     */
    COUNTERPARTY_NAME,

    /**
     * Match by UPI ID (e.g., name@okaxis, merchant@paytm).
     * Extracted from transaction description or counterparty details.
     */
    UPI_ID,

    /**
     * Match by mobile number in UPI transactions.
     * Common pattern: payments to 9876543210@upi or similar.
     */
    MOBILE_NUMBER,

    /**
     * Match by keyword in transaction description.
     * Uses case-insensitive contains matching.
     */
    DESCRIPTION_KEYWORD,

    /**
     * Match by merchant name.
     * Uses case-insensitive contains matching.
     */
    MERCHANT
}

