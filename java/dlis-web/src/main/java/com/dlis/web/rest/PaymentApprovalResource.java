package com.dlis.web.rest;

import com.dlis.core.domain.IssuedLicense;
import com.dlis.core.domain.Payment;
import com.dlis.core.service.ApprovalService;
import com.dlis.core.service.PaymentService;
import com.dlis.core.service.ServiceResult;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Resource: Payment and Approval workflow
 *
 * POST /api/v1/applications/{id}/payment      — process payment
 * POST /api/v1/applications/{id}/approval1    — first authority approval
 * POST /api/v1/applications/{id}/approval2    — second authority approval
 * POST /api/v1/applications/{id}/issue        — issue the license
 */
@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentApprovalResource {

    @Inject private PaymentService  paymentService;
    @Inject private ApprovalService approvalService;

    @POST
    @Path("/{id}/payment")
    public Response processPayment(@PathParam("id") Long id, PaymentRequest req) {
        if (req == null) return badRequest("Request body required");
        ServiceResult<Payment> result = paymentService.processPayment(
                id, req.candidateId, req.paymentMethod, req.paymentReference,
                nullOrDefault(req.processedBy, "API"));
        if (result.isOk()) {
            return Response.ok(new PaymentResponse(result.getPayload(),
                    result.getMessage())).build();
        }
        return errorResponse(result);
    }

    @POST
    @Path("/{id}/approval1")
    public Response approval1(@PathParam("id") Long id, ApprovalRequest req) {
        if (req == null) return badRequest("Request body required");
        ServiceResult<Void> result = approvalService.recordApproval1(
                id, req.authorityUserCode, req.decision, req.decisionNotes);
        return result.isOk()
                ? Response.ok(msg(result.getMessage())).build()
                : errorResponse(result);
    }

    @POST
    @Path("/{id}/approval2")
    public Response approval2(@PathParam("id") Long id, ApprovalRequest req) {
        if (req == null) return badRequest("Request body required");
        ServiceResult<Void> result = approvalService.recordApproval2(
                id, req.authorityUserCode, req.decision, req.decisionNotes);
        return result.isOk()
                ? Response.ok(msg(result.getMessage())).build()
                : errorResponse(result);
    }

    @POST
    @Path("/{id}/issue")
    public Response issueLicense(@PathParam("id") Long id, IssueRequest req) {
        if (req == null) return badRequest("Request body required");
        ServiceResult<IssuedLicense> result = approvalService.issueLicense(
                id, req.vehicleClass, req.restrictions,
                req.issuedByOfficer, req.issuedByAuthority);
        return result.isOk()
                ? Response.status(Response.Status.CREATED)
                    .entity(result.getPayload()).build()
                : errorResponse(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class PaymentRequest {
        public Long   candidateId;
        public String paymentMethod;
        public String paymentReference;
        public String processedBy;
    }

    public static class ApprovalRequest {
        public String authorityUserCode;
        public String decision;       // A or R
        public String decisionNotes;
    }

    public static class IssueRequest {
        public String vehicleClass;   // A, B, C, D
        public String restrictions;
        public String issuedByOfficer;
        public String issuedByAuthority;
    }

    public static class PaymentResponse {
        public Long   paymentId;
        public String receiptNumber;
        public String message;
        public PaymentResponse(Payment p, String m) {
            this.paymentId = p != null ? p.getPaymentId() : null;
            this.receiptNumber = p != null ? p.getReceiptNumber() : null;
            this.message = m;
        }
    }

    private static String nullOrDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private Response badRequest(String msg) {
        return Response.status(400).entity("{\"error\":\"" + msg + "\"}").build();
    }

    private <T> Response errorResponse(ServiceResult<T> r) {
        int code = r.getReturnCode() == ServiceResult.RC_NOT_FOUND ? 404 : 422;
        return Response.status(code)
                .entity("{\"returnCode\":" + r.getReturnCode()
                        + ",\"message\":\"" + r.getMessage().replace("\"","'") + "\"}")
                .build();
    }

    private Object msg(String m) {
        return "{\"message\":\"" + m.replace("\"","'") + "\"}";
    }
}
