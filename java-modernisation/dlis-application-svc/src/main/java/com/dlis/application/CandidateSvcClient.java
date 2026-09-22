package com.dlis.application;

import com.dlis.common.domain.Candidate;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for the Candidate Service.
 *
 * The base URL is configured in application.properties:
 *   quarkus.rest-client."com.dlis.application.CandidateSvcClient".url=http://dlis-candidate-svc:8080
 *
 * In OpenShift the service name resolves via internal DNS to the
 * dlis-candidate-svc Service object in the same namespace.
 */
@RegisterRestClient(configKey = "candidate-svc")
@Path("/api/v1/candidates")
@Produces(MediaType.APPLICATION_JSON)
public interface CandidateSvcClient {

    @GET
    @Path("/{id}")
    Candidate findById(@PathParam("id") Long candidateId);
}
