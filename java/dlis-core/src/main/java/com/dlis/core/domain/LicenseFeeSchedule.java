package com.dlis.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for LICENSE_FEE_SCHEDULE entity.
 * Maps to DLIS.LICENSE_FEE_SCHEDULE in DB2.
 */
public class LicenseFeeSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long        feeScheduleId;
    private String      licenseType;      // L, P, O
    private String      feeType;          // IF, RF, LF, PF
    private BigDecimal  feeAmount;
    private LocalDate   effectiveDate;
    private LocalDate   expiryDate;
    private String      currencyCode;
    private String      description;
    private String      activeStatus;     // A, I

    public LicenseFeeSchedule() {}

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return "A".equals(activeStatus)
            && !effectiveDate.isAfter(today)
            && (expiryDate == null || !expiryDate.isBefore(today));
    }

    // ---- Getters / Setters ----

    public Long getFeeScheduleId()                  { return feeScheduleId; }
    public void setFeeScheduleId(Long v)            { this.feeScheduleId = v; }

    public String getLicenseType()                  { return licenseType; }
    public void setLicenseType(String v)            { this.licenseType = v; }

    public String getFeeType()                      { return feeType; }
    public void setFeeType(String v)                { this.feeType = v; }

    public BigDecimal getFeeAmount()                { return feeAmount; }
    public void setFeeAmount(BigDecimal v)          { this.feeAmount = v; }

    public LocalDate getEffectiveDate()             { return effectiveDate; }
    public void setEffectiveDate(LocalDate v)       { this.effectiveDate = v; }

    public LocalDate getExpiryDate()                { return expiryDate; }
    public void setExpiryDate(LocalDate v)          { this.expiryDate = v; }

    public String getCurrencyCode()                 { return currencyCode; }
    public void setCurrencyCode(String v)           { this.currencyCode = v; }

    public String getDescription()                  { return description; }
    public void setDescription(String v)            { this.description = v; }

    public String getActiveStatus()                 { return activeStatus; }
    public void setActiveStatus(String v)           { this.activeStatus = v; }
}
