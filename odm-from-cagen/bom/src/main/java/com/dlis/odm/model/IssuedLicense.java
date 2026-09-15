package com.dlis.odm.model;

import java.time.LocalDate;

/**
 * BOM Class: IssuedLicense
 * Converted from CA Gen entity: ISSUED-LICENSE.ENT
 *
 * The issued driver license record — created as the final output of a
 * successfully completed application workflow.
 */
public class IssuedLicense {

    /** System-generated primary key (LICENSE-ID) */
    private long licenseId;

    /** Foreign key to LICENSE-APPLICATION (APPLICATION-ID) */
    private long applicationId;

    /** Foreign key to CANDIDATE (CANDIDATE-ID) */
    private long candidateId;

    /** Unique license number — system generated (LICENSE-NUMBER) */
    private String licenseNumber;

    /**
     * License category (LICENSE-TYPE).
     * Valid values: "L"=Learner, "P"=Probation, "O"=Open
     */
    private String licenseType;

    /** Date license was issued (ISSUE-DATE) */
    private LocalDate issueDate;

    /** License expiry date — must be after issueDate (EXPIRY-DATE) */
    private LocalDate expiryDate;

    /**
     * Current license status (LICENSE-STATUS).
     * Valid values: "A"=Active, "S"=Suspended, "E"=Expired, "C"=Cancelled, "R"=Revoked
     */
    private String licenseStatus;

    /**
     * Authorised vehicle class (VEHICLE-CLASS).
     * Valid values: "A"=Motorcycle, "B"=Light Vehicle, "C"=Heavy Vehicle, "D"=Bus/Coach
     */
    private String vehicleClass;

    /** Special conditions or restrictions (RESTRICTIONS) */
    private String restrictions;

    /**
     * Remaining demerit balance (DEMERIT-BALANCE).
     * Starts at 12 for all new licenses.
     */
    private int demeritBalance;

    /** Issuing authority organisation (ISSUED-BY-AUTHORITY) */
    private String issuedByAuthority;

    /** Issuing officer name (ISSUED-BY-OFFICER) */
    private String issuedByOfficer;

    /** Number of renewals (RENEWAL-COUNT) */
    private int renewalCount;

    /** Prior license ID for upgrade tracking (PREVIOUS-LICENSE-ID) */
    private Long previousLicenseId;

    /** Additional notes (NOTES) */
    private String notes;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getLicenseId() { return licenseId; }
    public void setLicenseId(long licenseId) { this.licenseId = licenseId; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getLicenseStatus() { return licenseStatus; }
    public void setLicenseStatus(String licenseStatus) { this.licenseStatus = licenseStatus; }

    public String getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(String vehicleClass) { this.vehicleClass = vehicleClass; }

    public String getRestrictions() { return restrictions; }
    public void setRestrictions(String restrictions) { this.restrictions = restrictions; }

    public int getDemeritBalance() { return demeritBalance; }
    public void setDemeritBalance(int demeritBalance) { this.demeritBalance = demeritBalance; }

    public String getIssuedByAuthority() { return issuedByAuthority; }
    public void setIssuedByAuthority(String issuedByAuthority) { this.issuedByAuthority = issuedByAuthority; }

    public String getIssuedByOfficer() { return issuedByOfficer; }
    public void setIssuedByOfficer(String issuedByOfficer) { this.issuedByOfficer = issuedByOfficer; }

    public int getRenewalCount() { return renewalCount; }
    public void setRenewalCount(int renewalCount) { this.renewalCount = renewalCount; }

    public Long getPreviousLicenseId() { return previousLicenseId; }
    public void setPreviousLicenseId(Long previousLicenseId) { this.previousLicenseId = previousLicenseId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
