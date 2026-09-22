package com.dlis.payment;

import com.dlis.common.api.ServiceResult;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for Payment processing.
 *
 * Migrated from {@code com.dlis.web.rest.PaymentApprovalResource} (payment portion).
 *
 * POST /api/v1/applications/{id}/payment  — process payment
 */
@Path("/api/v1/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/{id}/payment")
    public Response processPayment(@PathParam("id") Long id, PaymentRequest req) {
        if (req == null) return badRequest("Request body required");
        ServiceResult<PaymentService.PaymentRecord> result = paymentService.processPayment(
                id, req.candidateId, req.paymentMethod, req.paymentReference,
                req.processedBy != null ? req.processedBy : "API");
        if (result.isOk()) {
            PaymentService.PaymentRecord p = result.getPayload();
            return Response.ok(new PaymentResponse(p.paymentId, p.receiptNumber,
                    result.getMessage())).build();
        }
        return errorResponse(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class PaymentRequest {
        public Long   candidateId;
        public String paymentMethod;
        public String paymentReference;
        public String processedBy;
    }

    public static class PaymentResponse {
        public Long   paymentId;
        public String receiptNumber;
        public String message;
        public PaymentResponse(Long id, String rn, String m) {
            this.paymentId = id; this.receiptNumber = rn; this.message = m;
        }
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
