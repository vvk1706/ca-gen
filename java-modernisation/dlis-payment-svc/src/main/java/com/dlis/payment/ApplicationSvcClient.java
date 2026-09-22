package com.dlis.payment;

import com.dlis.common.domain.LicenseApplication;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for the Application Service.
 *
 * Configuration key in application.properties:
 *   quarkus.rest-client.application-svc.url=http://dlis-application-svc:8080
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
    @Path("/{id}/payment-status")
    void updatePaymentStatus(@PathParam("id") Long applicationId,
                             PaymentStatusRequest request);

    class PaymentStatusRequest {
        public String paymentStatus;
        public String paymentReference;
        public String applicationStatus;
        public String lastUpdatedDate;
        public String lastUpdatedBy;

        public PaymentStatusRequest(String paymentStatus, String paymentReference,
                                    String applicationStatus, String lastUpdatedDate,
                                    String lastUpdatedBy) {
            this.paymentStatus    = paymentStatus;
            this.paymentReference = paymentReference;
            this.applicationStatus = applicationStatus;
            this.lastUpdatedDate  = lastUpdatedDate;
            this.lastUpdatedBy    = lastUpdatedBy;
        }
    }
}
