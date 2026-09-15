package com.dlis.odm.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BOM Class: RuleContext
 *
 * The top-level fact object passed into every ODM ruleset execution.
 * Aggregates all inputs required by the DLIS ruleflows and accumulates
 * output fields (violations, status updates, calculated values).
 *
 * Analogous to the combined working-storage and local-view data that
 * CA Gen action blocks read and write during a single transaction.
 */
public class RuleContext {

    // ── Input facts ──────────────────────────────────────────────────────────

    /** The license application being processed */
    private LicenseApplication application;

    /** The candidate associated with the application */
    private Candidate candidate;

    /** All active driving history records for the candidate */
    private List<DrivingHistory> drivingHistoryRecords = new ArrayList<>();

    /** Active issued licenses held by the candidate (for upgrade path checks) */
    private List<IssuedLicense> existingLicenses = new ArrayList<>();

    /** The authority user submitting an approval decision */
    private AuthorityUser approvalUser;

    /** The active fee schedule entry for this application's license type */
    private LicenseFeeSchedule applicableFeeSchedule;

    // ── Approval request fields ───────────────────────────────────────────────

    /** Approval decision: "A"=Approve, "R"=Reject */
    private String approvalDecision;

    /** Notes accompanying the approval decision */
    private String approvalDecisionNotes;

    // ── Payment request fields ────────────────────────────────────────────────

    /** Payment method: CC, DC, EF, CS, CH */
    private String paymentMethod;

    /** External payment reference from bank/gateway */
    private String paymentReference;

    /** Officer processing the payment */
    private String processedBy;

    // ── Issuance request fields ───────────────────────────────────────────────

    /** Vehicle class: A=Motorcycle, B=Light, C=Heavy, D=Bus */
    private String vehicleClass;

    /** Special restrictions on the license */
    private String licenseRestrictions;

    /** Officer issuing the license */
    private String issuedByOfficer;

    /** Authority issuing the license */
    private String issuedByAuthority;

    // ── Rule output / result fields ───────────────────────────────────────────

    /**
     * Overall result code.
     * 0=Success, non-zero=failure (mirrors CA Gen return code convention).
     */
    private int returnCode;

    /** Human-readable result message */
    private String returnMessage;

    /** List of rule violations accumulated during a ruleset execution */
    private List<RuleViolation> violations = new ArrayList<>();

    /** Calculated expiry date — populated by the issuance ruleset */
    private LocalDate calculatedExpiryDate;

    /** Generated license number — populated by the issuance ruleset */
    private String generatedLicenseNumber;

    /** Generated receipt number — populated by the payment ruleset */
    private String generatedReceiptNumber;

    /** Calculated age of the candidate in years — populated by eligibility ruleset */
    private int candidateAge;

    /** Accumulated total demerit points — populated by history ruleset */
    private int totalDemeritPoints;

    /** Today's date — injected at invocation time for deterministic rule execution */
    private LocalDate today;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public LicenseApplication getApplication() { return application; }
    public void setApplication(LicenseApplication application) { this.application = application; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public List<DrivingHistory> getDrivingHistoryRecords() { return drivingHistoryRecords; }
    public void setDrivingHistoryRecords(List<DrivingHistory> records) { this.drivingHistoryRecords = records; }

    public List<IssuedLicense> getExistingLicenses() { return existingLicenses; }
    public void setExistingLicenses(List<IssuedLicense> existingLicenses) { this.existingLicenses = existingLicenses; }

    public AuthorityUser getApprovalUser() { return approvalUser; }
    public void setApprovalUser(AuthorityUser approvalUser) { this.approvalUser = approvalUser; }

    public LicenseFeeSchedule getApplicableFeeSchedule() { return applicableFeeSchedule; }
    public void setApplicableFeeSchedule(LicenseFeeSchedule applicableFeeSchedule) {
        this.applicableFeeSchedule = applicableFeeSchedule;
    }

    public String getApprovalDecision() { return approvalDecision; }
    public void setApprovalDecision(String approvalDecision) { this.approvalDecision = approvalDecision; }

    public String getApprovalDecisionNotes() { return approvalDecisionNotes; }
    public void setApprovalDecisionNotes(String approvalDecisionNotes) {
        this.approvalDecisionNotes = approvalDecisionNotes;
    }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public String getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(String vehicleClass) { this.vehicleClass = vehicleClass; }

    public String getLicenseRestrictions() { return licenseRestrictions; }
    public void setLicenseRestrictions(String licenseRestrictions) {
        this.licenseRestrictions = licenseRestrictions;
    }

    public String getIssuedByOfficer() { return issuedByOfficer; }
    public void setIssuedByOfficer(String issuedByOfficer) { this.issuedByOfficer = issuedByOfficer; }

    public String getIssuedByAuthority() { return issuedByAuthority; }
    public void setIssuedByAuthority(String issuedByAuthority) { this.issuedByAuthority = issuedByAuthority; }

    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }

    public String getReturnMessage() { return returnMessage; }
    public void setReturnMessage(String returnMessage) { this.returnMessage = returnMessage; }

    public List<RuleViolation> getViolations() { return violations; }
    public void setViolations(List<RuleViolation> violations) { this.violations = violations; }

    public void addViolation(RuleViolation violation) { this.violations.add(violation); }

    public boolean hasViolations() { return !violations.isEmpty(); }

    public LocalDate getCalculatedExpiryDate() { return calculatedExpiryDate; }
    public void setCalculatedExpiryDate(LocalDate calculatedExpiryDate) {
        this.calculatedExpiryDate = calculatedExpiryDate;
    }

    public String getGeneratedLicenseNumber() { return generatedLicenseNumber; }
    public void setGeneratedLicenseNumber(String generatedLicenseNumber) {
        this.generatedLicenseNumber = generatedLicenseNumber;
    }

    public String getGeneratedReceiptNumber() { return generatedReceiptNumber; }
    public void setGeneratedReceiptNumber(String generatedReceiptNumber) {
        this.generatedReceiptNumber = generatedReceiptNumber;
    }

    public int getCandidateAge() { return candidateAge; }
    public void setCandidateAge(int candidateAge) { this.candidateAge = candidateAge; }

    public int getTotalDemeritPoints() { return totalDemeritPoints; }
    public void setTotalDemeritPoints(int totalDemeritPoints) { this.totalDemeritPoints = totalDemeritPoints; }

    public LocalDate getToday() { return today; }
    public void setToday(LocalDate today) { this.today = today; }
}
