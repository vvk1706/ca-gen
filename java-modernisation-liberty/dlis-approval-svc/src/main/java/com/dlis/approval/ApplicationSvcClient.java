package com.dlis.approval;

import com.dlis.common.domain.LicenseApplication;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for the Application Service.
 *
 * Configured in microprofile-config.properties:
 *   application-svc/mp-rest/url=http://dlis-application-svc:9080
 */
@RegisterRestClient(configKey = "application-svc")
@Path("/api/v1/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ApplicationSvcClient {

    @GET
    @Path("/{id}")
    LicenseApplication findById(@PathParam("id") Long applicationId);

    @POST
    @Path("/{id}/approval1-status")
    void updateApproval1(@PathParam("id") Long applicationId, ApprovalRequest request);

    @POST
    @Path("/{id}/approval2-status")
    void updateApproval2(@PathParam("id") Long applicationId, ApprovalRequest request);

    @POST
    @Path("/{id}/status")
    void updateApplicationStatus(@PathParam("id") Long applicationId, StatusRequest request);

    class ApprovalRequest {
        public String decision;
        public String authorityName;
        public String decisionDate;
        public String decisionNotes;
        public String applicationStatus;
        public String lastUpdatedDate;
        public String lastUpdatedBy;

        public ApprovalRequest() {}

        public ApprovalRequest(String decision, String authorityName, String decisionDate,
                               String decisionNotes, String applicationStatus,
                               String lastUpdatedDate, String lastUpdatedBy) {
            this.decision          = decision;
            this.authorityName     = authorityName;
            this.decisionDate      = decisionDate;
            this.decisionNotes     = decisionNotes;
            this.applicationStatus = applicationStatus;
            this.lastUpdatedDate   = lastUpdatedDate;
            this.lastUpdatedBy     = lastUpdatedBy;
        }
    }

    class StatusRequest {
        public String applicationStatus;
        public String lastUpdatedDate;
        public String lastUpdatedBy;

        public StatusRequest() {}

        public StatusRequest(String applicationStatus, String lastUpdatedDate, String lastUpdatedBy) {
            this.applicationStatus = applicationStatus;
            this.lastUpdatedDate   = lastUpdatedDate;
            this.lastUpdatedBy     = lastUpdatedBy;
        }
    }
}
