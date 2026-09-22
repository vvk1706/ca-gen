package com.dlis.application;

import com.dlis.common.api.ServiceResult;
import com.dlis.common.domain.LicenseApplication;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for Application Lifecycle.
 *
 * Migrated from {@code com.dlis.web.rest.ApplicationResource} (monolith dlis-web).
 *
 * POST   /api/v1/applications                          — create application
 * GET    /api/v1/applications/{id}                    — inquire status
 * POST   /api/v1/applications/{id}/eligibility-check  — run eligibility check
 * POST   /api/v1/applications/{id}/history-check      — run history check
 */
@Path("/api/v1/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationResource {

    @Inject
    ApplicationService applicationService;

    @POST
    public Response createApplication(CreateApplicationRequest req) {
        if (req == null) return badRequest("Request body required");
        String userId = req.createdBy != null ? req.createdBy : "API";
        ServiceResult<Long> result =
                applicationService.createApplication(req.candidateId, req.licenseType, userId);
        if (result.isOk()) {
            return Response.status(Response.Status.CREATED)
                    .entity(new IdResponse(result.getPayload(), result.getMessage()))
                    .build();
        }
        return errorResponse(result);
    }

    @GET
    @Path("/{id}")
    public Response inquireStatus(@PathParam("id") Long id) {
        ServiceResult<LicenseApplication> result = applicationService.inquireStatus(id);
        return result.isOk()
                ? Response.ok(result.getPayload()).build()
                : errorResponse(result);
    }

    @POST
    @Path("/{id}/eligibility-check")
    public Response eligibilityCheck(@PathParam("id") Long id, CheckRequest req) {
        String userId = req != null && req.checkedBy != null ? req.checkedBy : "API";
        ServiceResult<String> result = applicationService.checkEligibility(id, userId);
        return result.isOk()
                ? Response.ok(new StatusResponse(result.getPayload(), result.getMessage())).build()
                : errorResponse(result);
    }

    @POST
    @Path("/{id}/history-check")
    public Response historyCheck(@PathParam("id") Long id, CheckRequest req) {
        String userId = req != null && req.checkedBy != null ? req.checkedBy : "API";
        ServiceResult<String> result = applicationService.checkHistory(id, userId);
        return result.isOk()
                ? Response.ok(new StatusResponse(result.getPayload(), result.getMessage())).build()
                : errorResponse(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class CreateApplicationRequest {
        public Long   candidateId;
        public String licenseType;
        public String createdBy;
    }

    public static class CheckRequest {
        public String checkedBy;
    }

    public static class IdResponse {
        public Long id; public String message;
        public IdResponse(Long id, String m) { this.id = id; this.message = m; }
    }

    public static class StatusResponse {
        public String status; public String message;
        public StatusResponse(String s, String m) { this.status = s; this.message = m; }
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
}
