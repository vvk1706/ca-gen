package com.dlis.odm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BOM Class: LicenseFeeSchedule
 * Converted from CA Gen entity: LICENSE-FEE-SCHEDULE.ENT
 *
 * Defines fee amounts per license type and fee category.
 * Supports effective date ranges for fee changes over time.
 * Evaluated during the payment processing ruleflow.
 */
public class LicenseFeeSchedule {

    /** System-generated primary key (FEE-SCHEDULE-ID) */
    private long feeScheduleId;

    /**
     * License type (LICENSE-TYPE).
     * Valid values: "L", "P", "O"
     */
    private String licenseType;

    /**
     * Fee category (FEE-TYPE).
     * Valid values: "IF"=Issue, "RF"=Renewal, "LF"=Late, "PF"=Processing
     */
    private String feeType;

    /** Amount in currency — must be > 0 (FEE-AMOUNT) */
    private BigDecimal feeAmount;

    /** Date fee becomes active (EFFECTIVE-DATE) */
    private LocalDate effectiveDate;

    /** Date fee expires — null means no expiry (EXPIRY-DATE) */
    private LocalDate expiryDate;

    /** ISO currency code (CURRENCY-CODE) — e.g., "USD", "AUD", "ZAR" */
    private String currencyCode;

    /** Fee description (DESCRIPTION) */
    private String description;

    /**
     * Active/Inactive flag (ACTIVE-STATUS).
     * Valid values: "A"=Active, "I"=Inactive
     */
    private String activeStatus;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getFeeScheduleId() { return feeScheduleId; }
    public void setFeeScheduleId(long feeScheduleId) { this.feeScheduleId = feeScheduleId; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }
}
