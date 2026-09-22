package com.dlis.core.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Domain model for the ISSUED_LICENSE entity.
 * Maps to DLIS.ISSUED_LICENSE in DB2.
 * Derived from CA Gen encyclopedia entity: ISSUED-LICENSE.ENT
 */
public class IssuedLicense implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE    = "A";
    public static final String STATUS_SUSPENDED = "S";
    public static final String STATUS_EXPIRED   = "E";
    public static final String STATUS_CANCELLED = "C";
    public static final String STATUS_REVOKED   = "R";

    private Long      licenseId;
    private Long      applicationId;
    private Long      candidateId;
    private String    licenseNumber;
    private String    licenseType;         // L, P, O
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String    licenseStatus;       // A, S, E, C, R
    private String    vehicleClass;        // A, B, C, D
    private String    restrictions;
    private Integer   demeritBalance;
    private String    issuedByAuthority;
    private String    issuedByOfficer;
    private Integer   renewalCount;
    private Long      previousLicenseId;
    private String    notes;

    public IssuedLicense() {}

    public boolean isActive()    { return STATUS_ACTIVE.equals(licenseStatus); }
    public boolean isExpired()   { return STATUS_EXPIRED.equals(licenseStatus); }

    // ---- Getters / Setters ----

    public Long getLicenseId()                      { return licenseId; }
    public void setLicenseId(Long v)                { this.licenseId = v; }

    public Long getApplicationId()                  { return applicationId; }
    public void setApplicationId(Long v)            { this.applicationId = v; }

    public Long getCandidateId()                    { return candidateId; }
    public void setCandidateId(Long v)              { this.candidateId = v; }

    public String getLicenseNumber()                { return licenseNumber; }
    public void setLicenseNumber(String v)          { this.licenseNumber = v; }

    public String getLicenseType()                  { return licenseType; }
    public void setLicenseType(String v)            { this.licenseType = v; }

    public LocalDate getIssueDate()                 { return issueDate; }
    public void setIssueDate(LocalDate v)           { this.issueDate = v; }

    public LocalDate getExpiryDate()                { return expiryDate; }
    public void setExpiryDate(LocalDate v)          { this.expiryDate = v; }

    public String getLicenseStatus()                { return licenseStatus; }
    public void setLicenseStatus(String v)          { this.licenseStatus = v; }

    public String getVehicleClass()                 { return vehicleClass; }
    public void setVehicleClass(String v)           { this.vehicleClass = v; }

    public String getRestrictions()                 { return restrictions; }
    public void setRestrictions(String v)           { this.restrictions = v; }

    public Integer getDemeritBalance()              { return demeritBalance; }
    public void setDemeritBalance(Integer v)        { this.demeritBalance = v; }

    public String getIssuedByAuthority()            { return issuedByAuthority; }
    public void setIssuedByAuthority(String v)      { this.issuedByAuthority = v; }

    public String getIssuedByOfficer()              { return issuedByOfficer; }
    public void setIssuedByOfficer(String v)        { this.issuedByOfficer = v; }

    public Integer getRenewalCount()                { return renewalCount; }
    public void setRenewalCount(Integer v)          { this.renewalCount = v; }

    public Long getPreviousLicenseId()              { return previousLicenseId; }
    public void setPreviousLicenseId(Long v)        { this.previousLicenseId = v; }

    public String getNotes()                        { return notes; }
    public void setNotes(String v)                  { this.notes = v; }
}
