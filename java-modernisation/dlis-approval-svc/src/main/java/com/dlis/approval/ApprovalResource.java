package com.dlis.approval;

import com.dlis.common.api.ServiceResult;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for Approval and License Issuance.
 *
 * Migrated from the approval portion of
 * {@code com.dlis.web.rest.PaymentApprovalResource} (monolith dlis-web).
 *
 * POST /api/v1/applications/{id}/approval1   — first authority approval
 * POST /api/v1/applications/{id}/approval2   — second authority approval
 * POST /api/v1/applications/{id}/issue       — issue the license
 */
@Path("/api/v1/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject
    ApprovalService approvalService;

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
        ServiceResult<ApprovalService.IssuedLicenseRecord> result = approvalService.issueLicense(
                id, req.vehicleClass, req.restrictions,
                req.issuedByOfficer, req.issuedByAuthority);
        return result.isOk()
                ? Response.status(Response.Status.CREATED).entity(result.getPayload()).build()
                : errorResponse(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

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

    private Response badRequest(String msg) {
        return Response.status(400).entity("{\"error\":\"" + msg + "\"}").build();
    }

    private <T> Response errorResponse(ServiceResult<T> r) {
        int code = r.getReturnCode() == ServiceResult.RC_NOT_FOUND ? 404 : 422;
        return Response.status(code)
                .entity("{\"returnCode\":" + r.getReturnCode()
                        + ",\"message\":\"" + r.getMessage().replace("\"", "'") + "\"}")
                .build();
    }

    private Object msg(String m) {
        return "{\"message\":\"" + m.replace("\"", "'") + "\"}";
    }
}
