package com.dlis.web.rest;

import com.dlis.core.domain.Candidate;
import com.dlis.core.service.CandidateService;
import com.dlis.core.service.ServiceResult;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;

/**
 * REST Resource: Candidate
 *
 * POST   /api/v1/candidates         — create candidate
 * GET    /api/v1/candidates/{id}    — get by primary key
 * GET    /api/v1/candidates?idNumber=... — find by national ID
 */
@Path("/candidates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidateResource {

    @Inject
    private CandidateService candidateService;

    @POST
    public Response createCandidate(CandidateRequest req) {
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Request body is required")).build();
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
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Invalid dateOfBirth format — use YYYY-MM-DD")).build();
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
        if (result.isOk()) {
            return Response.ok(result.getPayload()).build();
        }
        return errorResponse(result);
    }

    @GET
    public Response findByIdNumber(@QueryParam("idNumber") String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("idNumber query parameter is required")).build();
        }
        ServiceResult<Candidate> result = candidateService.findByIdNumber(idNumber);
        if (result.isOk()) {
            return Response.ok(result.getPayload()).build();
        }
        return errorResponse(result);
    }

    // ── Request/Response DTOs ─────────────────────────────────────────────────

    public static class CandidateRequest {
        public String firstName;
        public String lastName;
        public String dateOfBirth;    // ISO-8601
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

    private static String errorBody(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }

    private static <T> Response errorResponse(ServiceResult<T> r) {
        int httpStatus = r.getReturnCode() == ServiceResult.RC_NOT_FOUND
                ? Response.Status.NOT_FOUND.getStatusCode()
                : Response.Status.UNPROCESSABLE_ENTITY.getStatusCode();
        return Response.status(httpStatus)
                .entity("{\"returnCode\":" + r.getReturnCode()
                        + ",\"message\":\"" + r.getMessage().replace("\"", "'") + "\"}")
                .build();
    }
}
