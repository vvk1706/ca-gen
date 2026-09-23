package com.dlis.common.domain;

import java.time.LocalDate;

/**
 * License Application domain object — shared across all microservices.
 * Application status lifecycle:
 *   PE → EC → HC → PP → PA → A1 → A2 → AP → IS
 *                                          ↓ (at any step)
 *                                         RE (rejected)
 */
public class LicenseApplication {

    // Status constants
    public static final String STATUS_PENDING        = "PE";
    public static final String STATUS_ELIG_CHECKED   = "EC";
    public static final String STATUS_HIST_CHECKED   = "HC";
    public static final String STATUS_PAYMENT_APPROVED = "PP";
    public static final String STATUS_PRE_APPROVED   = "PA";
    public static final String STATUS_APPROVAL1_PEND = "A1";
    public static final String STATUS_APPROVAL2_PEND = "A2";
    public static final String STATUS_APPROVED       = "AP";
    public static final String STATUS_ISSUED         = "IS";
    public static final String STATUS_REJECTED       = "RE";

    public static final String CHECK_UNCHECKED = "U";
    public static final String CHECK_PASSED    = "P";
    public static final String CHECK_FAILED    = "F";

    public static final String PAYMENT_UNPAID  = "U";
    public static final String PAYMENT_PAID    = "P";
    public static final String PAYMENT_WAIVED  = "W";

    // Fields
    private Long      applicationId;
    private Long      candidateId;
    private String    licenseType;   // L, P, O
    private LocalDate applicationDate;
    private String    applicationStatus;
    private String    eligibilityChkStatus;
    private LocalDate eligibilityChkDate;
    private String    eligibilityChkNotes;
    private String    historyChkStatus;
    private LocalDate historyChkDate;
    private String    historyChkNotes;
    private String    paymentStatus;
    private String    paymentReference;
    private String    approval1Status;
    private String    approval1Authority;
    private LocalDate approval1Date;
    private String    approval1Notes;
    private String    approval2Status;
    private String    approval2Authority;
    private LocalDate approval2Date;
    private String    approval2Notes;
    private String    rejectionReason;
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
    private String    createdBy;
    private String    lastUpdatedBy;

    // ── Convenience predicates ────────────────────────────────────────────────

    public boolean isEligibilityPassed() { return CHECK_PASSED.equals(eligibilityChkStatus); }
    public boolean isHistoryPassed()     { return CHECK_PASSED.equals(historyChkStatus); }
    public boolean isPaymentComplete()   { return PAYMENT_PAID.equals(paymentStatus); }
    public boolean isFullyApproved()     { return STATUS_APPROVED.equals(applicationStatus); }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long      getApplicationId()     { return applicationId; }
    public void      setApplicationId(Long v){ this.applicationId = v; }

    public Long      getCandidateId()       { return candidateId; }
    public void      setCandidateId(Long v) { this.candidateId = v; }

    public String    getLicenseType()       { return licenseType; }
    public void      setLicenseType(String v){ this.licenseType = v; }

    public LocalDate getApplicationDate()   { return applicationDate; }
    public void      setApplicationDate(LocalDate v){ this.applicationDate = v; }

    public String    getApplicationStatus() { return applicationStatus; }
    public void      setApplicationStatus(String v){ this.applicationStatus = v; }

    public String    getEligibilityChkStatus() { return eligibilityChkStatus; }
    public void      setEligibilityChkStatus(String v){ this.eligibilityChkStatus = v; }

    public LocalDate getEligibilityChkDate(){ return eligibilityChkDate; }
    public void      setEligibilityChkDate(LocalDate v){ this.eligibilityChkDate = v; }

    public String    getEligibilityChkNotes(){ return eligibilityChkNotes; }
    public void      setEligibilityChkNotes(String v){ this.eligibilityChkNotes = v; }

    public String    getHistoryChkStatus()  { return historyChkStatus; }
    public void      setHistoryChkStatus(String v){ this.historyChkStatus = v; }

    public LocalDate getHistoryChkDate()    { return historyChkDate; }
    public void      setHistoryChkDate(LocalDate v){ this.historyChkDate = v; }

    public String    getHistoryChkNotes()   { return historyChkNotes; }
    public void      setHistoryChkNotes(String v){ this.historyChkNotes = v; }

    public String    getPaymentStatus()     { return paymentStatus; }
    public void      setPaymentStatus(String v){ this.paymentStatus = v; }

    public String    getPaymentReference()  { return paymentReference; }
    public void      setPaymentReference(String v){ this.paymentReference = v; }

    public String    getApproval1Status()   { return approval1Status; }
    public void      setApproval1Status(String v){ this.approval1Status = v; }

    public String    getApproval1Authority(){ return approval1Authority; }
    public void      setApproval1Authority(String v){ this.approval1Authority = v; }

    public LocalDate getApproval1Date()     { return approval1Date; }
    public void      setApproval1Date(LocalDate v){ this.approval1Date = v; }

    public String    getApproval1Notes()    { return approval1Notes; }
    public void      setApproval1Notes(String v){ this.approval1Notes = v; }

    public String    getApproval2Status()   { return approval2Status; }
    public void      setApproval2Status(String v){ this.approval2Status = v; }

    public String    getApproval2Authority(){ return approval2Authority; }
    public void      setApproval2Authority(String v){ this.approval2Authority = v; }

    public LocalDate getApproval2Date()     { return approval2Date; }
    public void      setApproval2Date(LocalDate v){ this.approval2Date = v; }

    public String    getApproval2Notes()    { return approval2Notes; }
    public void      setApproval2Notes(String v){ this.approval2Notes = v; }

    public String    getRejectionReason()   { return rejectionReason; }
    public void      setRejectionReason(String v){ this.rejectionReason = v; }

    public LocalDate getCreatedDate()       { return createdDate; }
    public void      setCreatedDate(LocalDate v){ this.createdDate = v; }

    public LocalDate getLastUpdatedDate()   { return lastUpdatedDate; }
    public void      setLastUpdatedDate(LocalDate v){ this.lastUpdatedDate = v; }

    public String    getCreatedBy()         { return createdBy; }
    public void      setCreatedBy(String v) { this.createdBy = v; }

    public String    getLastUpdatedBy()     { return lastUpdatedBy; }
    public void      setLastUpdatedBy(String v){ this.lastUpdatedBy = v; }
}
