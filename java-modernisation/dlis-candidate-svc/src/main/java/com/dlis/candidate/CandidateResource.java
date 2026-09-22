package com.dlis.candidate;

import com.dlis.common.api.ServiceResult;
import com.dlis.common.domain.Candidate;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

/**
 * REST resource for Candidate management.
 *
 * Migrated from {@code com.dlis.web.rest.CandidateResource} (monolith dlis-web).
 * EJB @Inject → CDI @Inject (Quarkus CDI).
 * javax.ws.rs → jakarta.ws.rs (Jakarta EE 10 / Quarkus 3.x).
 *
 * Base path:  /api/v1/candidates
 *
 * POST   /api/v1/candidates             — create candidate
 * GET    /api/v1/candidates/{id}        — get by primary key
 * GET    /api/v1/candidates?idNumber=   — find by national ID number
 */
@Path("/api/v1/candidates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidateResource {

    @Inject
    CandidateService candidateService;

    @POST
    public Response createCandidate(CandidateRequest req) {
        if (req == null) {
            return badRequest("Request body is required");
        }

        Candidate c = new Candidate();
        c.setFirstName(req.firstName);
        c.setLastName(req.lastName);
        c.setIdNumber(req.idNumber);
        c.setAddressLine1(req.addressLine1);
        c.setAddressLine2(req.addressLine2);
        c.setCity(req.city);
        c.setStateProvince(req.stateProvince);
        c.setPostalCode(req.postalCode);
        c.setCountry(req.country);
        c.setPhoneNumber(req.phoneNumber);
        c.setEmailAddress(req.emailAddress);

        try {
            c.setDateOfBirth(LocalDate.parse(req.dateOfBirth));
        } catch (Exception e) {
            return badRequest("Invalid dateOfBirth format — use YYYY-MM-DD");
        }

        String userId = req.createdBy != null ? req.createdBy : "API";
        ServiceResult<Long> result = candidateService.createCandidate(c, userId);

        if (result.isOk()) {
            return Response.status(Response.Status.CREATED)
                    .entity(new IdResponse(result.getPayload(), result.getMessage()))
                    .build();
        }
        return errorResponse(result);
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        ServiceResult<Candidate> result = candidateService.findById(id);
        return result.isOk()
                ? Response.ok(result.getPayload()).build()
                : errorResponse(result);
    }

    @GET
    public Response findByIdNumber(@QueryParam("idNumber") String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return badRequest("idNumber query parameter is required");
        }
        ServiceResult<Candidate> result = candidateService.findByIdNumber(idNumber);
        return result.isOk()
                ? Response.ok(result.getPayload()).build()
                : errorResponse(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class CandidateRequest {
        public String firstName;
        public String lastName;
        public String dateOfBirth;   // ISO-8601 YYYY-MM-DD
        public String idNumber;
        public String addressLine1;
        public String addressLine2;
        public String city;
        public String stateProvince;
        public String postalCode;
        public String country;
        public String phoneNumber;
        public String emailAddress;
        public String createdBy;
    }

    public static class IdResponse {
        public Long   id;
        public String message;
        public IdResponse(Long id, String msg) { this.id = id; this.message = msg; }
    }

    private Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + msg.replace("\"", "'") + "\"}")
                .build();
    }

    private <T> Response errorResponse(ServiceResult<T> r) {
        int code = r.getReturnCode() == ServiceResult.RC_NOT_FOUND ? 404 : 422;
        return Response.status(code)
                .entity("{\"returnCode\":" + r.getReturnCode()
                        + ",\"message\":\"" + r.getMessage().replace("\"", "'") + "\"}")
                .build();
    }
}
