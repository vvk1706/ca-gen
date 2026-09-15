package com.dlis.odm.rest;

import com.dlis.odm.model.*;
import com.dlis.odm.service.DlisRuleExecutionService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;

/**
 * DlisRuleResource
 *
 * JAX-RS REST API for the DLIS ODM application.
 *
 * Exposes each workflow stage as an individual REST endpoint.
 * Each endpoint:
 *   1. Accepts a JSON request body
 *   2. Calls the CICS DLISQLUP program to load rule context from DB2
 *   3. Invokes the appropriate ODM ruleset via RES REST API
 *   4. On success: calls the appropriate CICS adapter (DLISPAY / DLISLICS)
 *      to perform transactional DB2 writes
 *   5. Returns the result as JSON
 *
 * Base path: /api/dlis
 *
 * Converted from CA Gen trigger-to-action-block invocations:
 *   TRG-CHECK-AND-STATUS → POST /eligibility-check, POST /history-check
 *   TRG-PAYMENT-APPROVAL-ISSUE → POST /payment, POST /approval/1, POST /approval/2, POST /issue
 *   TRG-CANDIDATE-MAINT → POST /candidates
 *   TRG-APPLICATION-ENTRY → POST /applications
 */
@Path("/dlis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DlisRuleResource {

    private final DlisRuleExecutionService ruleService;

    public DlisRuleResource(DlisRuleExecutionService ruleService) {
        this.ruleService = ruleService;
    }

    // ── POST /candidates ────────────────────────────────────────────────────
    /**
     * Register a new candidate.
     * Corresponds to: AB-CREATE-CANDIDATE
     * ODM Ruleset: candidate/candidate-rules
     * After rules pass: CICS program DLISCAND performs DB2 INSERT.
     */
    @POST
    @Path("/candidates")
    public Response createCandidate(CreateCandidateRequest request) {
        RuleContext context = new RuleContext();
        context.setToday(LocalDate.now());

        Candidate candidate = new Candidate();
        candidate.setFirstName(request.getFirstName());
        candidate.setLastName(request.getLastName());
        candidate.setDateOfBirth(request.getDateOfBirth());
        candidate.setIdNumber(request.getIdNumber());
        candidate.setAddressLine1(request.getAddressLine1());
        candidate.setCity(request.getCity());
        candidate.setStateProvince(request.getStateProvince());
        candidate.setCountry(request.getCountry());
        candidate.setRecordStatus("A");
        context.setCandidate(candidate);

        RuleContext result = ruleService.executeRuleset("candidate/candidate-rules", context);
        return toResponse(result);
    }

    // ── POST /applications ──────────────────────────────────────────────────
    /**
     * Create a license application for an existing candidate.
     * Corresponds to: AB-CREATE-APPLICATION
     * ODM Ruleset: candidate/candidate-rules (application creation rules)
     * After rules pass: CICS program DLISAPPL performs DB2 INSERT.
     */
    @POST
    @Path("/applications")
    public Response createApplication(CreateApplicationRequest request) {
        // Load candidate from DB2 via CICS lookup
        RuleContext context = ruleService.loadContextForCandidate(
            request.getCandidateId(), request.getLicenseType());
        context.setToday(LocalDate.now());

        RuleContext result = ruleService.executeRuleset("candidate/candidate-rules", context);
        return toResponse(result);
    }

    // ── POST /applications/{id}/eligibility-check ───────────────────────────
    /**
     * Run the eligibility check for an application.
     * Corresponds to: AB-CHECK-ELIGIBILITY
     * ODM Ruleset: eligibility/eligibility-rules
     * DB2 UPDATE performed via CICS after rules pass.
     */
    @POST
    @Path("/applications/{applicationId}/eligibility-check")
    public Response runEligibilityCheck(
            @PathParam("applicationId") long applicationId,
            EligibilityCheckRequest request) {

        // Load full context from mainframe via DLISQLUP
        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());

        RuleContext result = ruleService.executeRuleset("eligibility/eligibility-rules", context);

        if (result.getReturnCode() == 0) {
            // Persist status update to DB2 via CICS (DLISUPD)
            ruleService.persistApplicationStatus(result);
        }

        return toResponse(result);
    }

    // ── POST /applications/{id}/history-check ───────────────────────────────
    /**
     * Run the driving history check.
     * Corresponds to: AB-CHECK-HISTORY
     * ODM Ruleset: history/history-rules
     */
    @POST
    @Path("/applications/{applicationId}/history-check")
    public Response runHistoryCheck(
            @PathParam("applicationId") long applicationId,
            HistoryCheckRequest request) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());

        // Also load driving history records
        ruleService.loadDrivingHistory(context);

        RuleContext result = ruleService.executeRuleset("history/history-rules", context);

        if (result.getReturnCode() == 0 || result.hasViolations()) {
            ruleService.persistApplicationStatus(result);
        }

        return toResponse(result);
    }

    // ── POST /applications/{id}/payment ─────────────────────────────────────
    /**
     * Process payment for an application.
     * Corresponds to: AB-PROCESS-PAYMENT
     * ODM Ruleset: payment/payment-rules
     * After rules pass: CICS DLISPAY performs DB2 INSERT (PAYMENT) + UPDATE (APPLICATION).
     */
    @POST
    @Path("/applications/{applicationId}/payment")
    public Response processPayment(
            @PathParam("applicationId") long applicationId,
            PaymentRequest request) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());
        context.setPaymentMethod(request.getPaymentMethod());
        context.setPaymentReference(request.getPaymentReference());
        context.setProcessedBy(request.getProcessedBy());

        RuleContext result = ruleService.executeRuleset("payment/payment-rules", context);

        if (result.getReturnCode() == 0 && !result.hasViolations()) {
            // Transactional write via CICS DLISPAY (mainframe DB2)
            ruleService.executePaymentViaMainframe(result);
        }

        return toResponse(result);
    }

    // ── POST /applications/{id}/approvals/1 ─────────────────────────────────
    /**
     * Submit first authority approval.
     * Corresponds to: AB-RECORD-APPROVAL-1
     * ODM Ruleset: approval/approval-rules (agenda-group: approval-level1-*)
     */
    @POST
    @Path("/applications/{applicationId}/approvals/1")
    public Response submitApproval1(
            @PathParam("applicationId") long applicationId,
            ApprovalRequest request) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());
        ruleService.loadAuthorityUser(context, request.getAuthorityUserCode());
        context.setApprovalDecision(request.getDecision());
        context.setApprovalDecisionNotes(request.getNotes());

        RuleContext result = ruleService.executeRuleset("approval/approval-rules", context,
            "approval-level1-gate", "approval-level1-validate", "record-approval1-result");

        if (result.getReturnCode() == 0) {
            ruleService.persistApplicationStatus(result);
        }

        return toResponse(result);
    }

    // ── POST /applications/{id}/approvals/2 ─────────────────────────────────
    /**
     * Submit second authority approval.
     * Corresponds to: AB-RECORD-APPROVAL-2
     * ODM Ruleset: approval/approval-rules (agenda-group: approval-level2-*)
     */
    @POST
    @Path("/applications/{applicationId}/approvals/2")
    public Response submitApproval2(
            @PathParam("applicationId") long applicationId,
            ApprovalRequest request) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());
        ruleService.loadAuthorityUser(context, request.getAuthorityUserCode());
        context.setApprovalDecision(request.getDecision());
        context.setApprovalDecisionNotes(request.getNotes());

        RuleContext result = ruleService.executeRuleset("approval/approval-rules", context,
            "approval-level2-gate", "approval-level2-validate", "record-approval2-result");

        if (result.getReturnCode() == 0) {
            ruleService.persistApplicationStatus(result);
        }

        return toResponse(result);
    }

    // ── POST /applications/{id}/issue ───────────────────────────────────────
    /**
     * Issue the driver license.
     * Corresponds to: AB-ISSUE-LICENSE
     * ODM Rulesets: issuance/issuance-rules
     * After rules pass: CICS DLISLICS performs DB2 INSERT (ISSUED_LICENSE) + UPDATE (APPLICATION).
     */
    @POST
    @Path("/applications/{applicationId}/issue")
    public Response issueLicense(
            @PathParam("applicationId") long applicationId,
            IssuanceRequest request) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        context.setToday(LocalDate.now());
        context.setVehicleClass(request.getVehicleClass());
        context.setLicenseRestrictions(request.getRestrictions());
        context.setIssuedByOfficer(request.getIssuedByOfficer());
        context.setIssuedByAuthority(request.getIssuedByAuthority());

        RuleContext result = ruleService.executeRuleset("issuance/issuance-rules", context);

        if (result.getReturnCode() == 0 && !result.hasViolations()) {
            // Transactional writes via CICS DLISLICS (mainframe DB2)
            ruleService.executeIssuanceViaMainframe(result);
        }

        return toResponse(result);
    }

    // ── GET /applications/{id}/status ───────────────────────────────────────
    /**
     * Inquire application status.
     * Corresponds to: AB-INQUIRE-APPLICATION-STATUS
     * No ODM rules — direct DB2 read via CICS DLISQLUP.
     */
    @GET
    @Path("/applications/{applicationId}/status")
    public Response getApplicationStatus(
            @PathParam("applicationId") long applicationId) {

        RuleContext context = ruleService.loadContextForApplication(applicationId);
        if (context == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"APPLICATION NOT FOUND\"}")
                .build();
        }
        return Response.ok(context.getApplication()).build();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Response toResponse(RuleContext context) {
        if (context.getReturnCode() == 0 && !context.hasViolations()) {
            return Response.ok(new RuleResult(context)).build();
        } else {
            return Response.status(Response.Status.UNPROCESSABLE_ENTITY)
                .entity(new RuleResult(context))
                .build();
        }
    }

    // ── Inner request/response DTOs ──────────────────────────────────────────

    public static class CreateCandidateRequest {
        private String firstName, lastName, idNumber, addressLine1, city, stateProvince, country;
        private LocalDate dateOfBirth;
        // getters/setters omitted for brevity — generated by IDE
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public String getIdNumber() { return idNumber; }
        public String getAddressLine1() { return addressLine1; }
        public String getCity() { return city; }
        public String getStateProvince() { return stateProvince; }
        public String getCountry() { return country; }
    }

    public static class CreateApplicationRequest {
        private long candidateId;
        private String licenseType;
        public long getCandidateId() { return candidateId; }
        public String getLicenseType() { return licenseType; }
    }

    public static class EligibilityCheckRequest {
        private String checkedBy;
        public String getCheckedBy() { return checkedBy; }
    }

    public static class HistoryCheckRequest {
        private String checkedBy;
        public String getCheckedBy() { return checkedBy; }
    }

    public static class PaymentRequest {
        private String paymentMethod, paymentReference, processedBy;
        public String getPaymentMethod() { return paymentMethod; }
        public String getPaymentReference() { return paymentReference; }
        public String getProcessedBy() { return processedBy; }
    }

    public static class ApprovalRequest {
        private String authorityUserCode, decision, notes;
        public String getAuthorityUserCode() { return authorityUserCode; }
        public String getDecision() { return decision; }
        public String getNotes() { return notes; }
    }

    public static class IssuanceRequest {
        private String vehicleClass, restrictions, issuedByOfficer, issuedByAuthority;
        public String getVehicleClass() { return vehicleClass; }
        public String getRestrictions() { return restrictions; }
        public String getIssuedByOfficer() { return issuedByOfficer; }
        public String getIssuedByAuthority() { return issuedByAuthority; }
    }

    public static class RuleResult {
        public final int returnCode;
        public final String returnMessage;
        public final java.util.List<RuleViolation> violations;
        public final String licenseNumber;
        public final LocalDate expiryDate;
        public final String receiptNumber;

        public RuleResult(RuleContext ctx) {
            this.returnCode    = ctx.getReturnCode();
            this.returnMessage = ctx.getReturnMessage();
            this.violations    = ctx.getViolations();
            this.licenseNumber = ctx.getGeneratedLicenseNumber();
            this.expiryDate    = ctx.getCalculatedExpiryDate();
            this.receiptNumber = ctx.getGeneratedReceiptNumber();
        }
    }
}
