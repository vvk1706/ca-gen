package com.dlis.odm.model;

import java.time.LocalDate;

/**
 * BOM Class: LicenseApplication
 * Converted from CA Gen entity: LICENSE-APPLICATION.ENT
 *
 * Tracks a driver license application from submission through to issuance.
 * This is the central fact object passed through all ODM ruleflows.
 *
 * Application Status lifecycle:
 *   PE → EC → HC → PA → A2 → AP → IS  (happy path)
 *   Any stage → RE                      (rejection)
 */
public class LicenseApplication {

    /** System-generated primary key (APPLICATION-ID) */
    private long applicationId;

    /** Foreign key to CANDIDATE (CANDIDATE-ID) */
    private long candidateId;

    /**
     * License category (LICENSE-TYPE).
     * Valid values: "L"=Learner, "P"=Probation, "O"=Open
     */
    private String licenseType;

    /** Date application was submitted (APPLICATION-DATE) */
    private LocalDate applicationDate;

    /**
     * Current workflow status (APPLICATION-STATUS).
     * Valid values: PE, EC, HC, PA, A2, AP, IS, RE
     */
    private String applicationStatus;

    /**
     * Eligibility check result (ELIGIBILITY-CHECK-STATUS).
     * Valid values: "P"=Pass, "F"=Fail, "U"=Unchecked
     */
    private String eligibilityCheckStatus;

    /** Date eligibility was checked (ELIGIBILITY-CHECK-DATE) */
    private LocalDate eligibilityCheckDate;

    /** Eligibility notes / failure reason (ELIGIBILITY-CHECK-NOTES) */
    private String eligibilityCheckNotes;

    /**
     * History check result (HISTORY-CHECK-STATUS).
     * Valid values: "P"=Pass, "F"=Fail, "U"=Unchecked
     */
    private String historyCheckStatus;

    /** Date history was checked (HISTORY-CHECK-DATE) */
    private LocalDate historyCheckDate;

    /** History check notes / failure reason (HISTORY-CHECK-NOTES) */
    private String historyCheckNotes;

    /**
     * Payment result (PAYMENT-STATUS).
     * Valid values: "P"=Paid, "U"=Unpaid, "W"=Waived
     */
    private String paymentStatus;

    /** Payment reference number (PAYMENT-REFERENCE) */
    private String paymentReference;

    /**
     * First approval result (APPROVAL-1-STATUS).
     * Valid values: "A"=Approved, "R"=Rejected, "U"=Unchecked
     */
    private String approval1Status;

    /** Name of the first approving authority (APPROVAL-1-AUTHORITY) */
    private String approval1Authority;

    /** Date of first approval (APPROVAL-1-DATE) */
    private LocalDate approval1Date;

    /** First approval notes (APPROVAL-1-NOTES) */
    private String approval1Notes;

    /**
     * Second approval result (APPROVAL-2-STATUS).
     * Valid values: "A"=Approved, "R"=Rejected, "U"=Unchecked
     */
    private String approval2Status;

    /** Name of the second approving authority (APPROVAL-2-AUTHORITY) */
    private String approval2Authority;

    /** Date of second approval (APPROVAL-2-DATE) */
    private LocalDate approval2Date;

    /** Second approval notes (APPROVAL-2-NOTES) */
    private String approval2Notes;

    /** Rejection reason if status = RE (REJECTION-REASON) */
    private String rejectionReason;

    /** Record creation date (CREATED-DATE) */
    private LocalDate createdDate;

    /** Last update date (LAST-UPDATED-DATE) */
    private LocalDate lastUpdatedDate;

    /** User ID who created the record (CREATED-BY) */
    private String createdBy;

    /** User ID of last update (LAST-UPDATED-BY) */
    private String lastUpdatedBy;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }

    public String getEligibilityCheckStatus() { return eligibilityCheckStatus; }
    public void setEligibilityCheckStatus(String eligibilityCheckStatus) { this.eligibilityCheckStatus = eligibilityCheckStatus; }

    public LocalDate getEligibilityCheckDate() { return eligibilityCheckDate; }
    public void setEligibilityCheckDate(LocalDate eligibilityCheckDate) { this.eligibilityCheckDate = eligibilityCheckDate; }

    public String getEligibilityCheckNotes() { return eligibilityCheckNotes; }
    public void setEligibilityCheckNotes(String eligibilityCheckNotes) { this.eligibilityCheckNotes = eligibilityCheckNotes; }

    public String getHistoryCheckStatus() { return historyCheckStatus; }
    public void setHistoryCheckStatus(String historyCheckStatus) { this.historyCheckStatus = historyCheckStatus; }

    public LocalDate getHistoryCheckDate() { return historyCheckDate; }
    public void setHistoryCheckDate(LocalDate historyCheckDate) { this.historyCheckDate = historyCheckDate; }

    public String getHistoryCheckNotes() { return historyCheckNotes; }
    public void setHistoryCheckNotes(String historyCheckNotes) { this.historyCheckNotes = historyCheckNotes; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getApproval1Status() { return approval1Status; }
    public void setApproval1Status(String approval1Status) { this.approval1Status = approval1Status; }

    public String getApproval1Authority() { return approval1Authority; }
    public void setApproval1Authority(String approval1Authority) { this.approval1Authority = approval1Authority; }

    public LocalDate getApproval1Date() { return approval1Date; }
    public void setApproval1Date(LocalDate approval1Date) { this.approval1Date = approval1Date; }

    public String getApproval1Notes() { return approval1Notes; }
    public void setApproval1Notes(String approval1Notes) { this.approval1Notes = approval1Notes; }

    public String getApproval2Status() { return approval2Status; }
    public void setApproval2Status(String approval2Status) { this.approval2Status = approval2Status; }

    public String getApproval2Authority() { return approval2Authority; }
    public void setApproval2Authority(String approval2Authority) { this.approval2Authority = approval2Authority; }

    public LocalDate getApproval2Date() { return approval2Date; }
    public void setApproval2Date(LocalDate approval2Date) { this.approval2Date = approval2Date; }

    public String getApproval2Notes() { return approval2Notes; }
    public void setApproval2Notes(String approval2Notes) { this.approval2Notes = approval2Notes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public LocalDate getLastUpdatedDate() { return lastUpdatedDate; }
    public void setLastUpdatedDate(LocalDate lastUpdatedDate) { this.lastUpdatedDate = lastUpdatedDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }
}
