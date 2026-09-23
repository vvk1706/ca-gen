package com.dlis.payment;

import com.dlis.common.api.ServiceResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Payment Service — Open Liberty CDI bean.
 *
 * Implements CA Gen action block: AB-PROCESS-PAYMENT.
 *
 * Inter-service call: application-svc REST client is used to verify
 * application status before recording payment.
 */
@ApplicationScoped
public class PaymentService {

    @Resource(lookup = "jdbc/dlisDS")
    DataSource dataSource;

    @Inject
    @RestClient
    ApplicationSvcClient applicationSvcClient;

    public ServiceResult<PaymentRecord> processPayment(Long applicationId,
                                                        Long candidateId,
                                                        String paymentMethod,
                                                        String paymentReference,
                                                        String processedBy) {
        // 1. Load application from application-svc
        var app = applicationSvcClient.findById(applicationId);
        if (app == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }

        // 2. History must be passed
        if (!app.isHistoryPassed()) {
            return ServiceResult.error(ServiceResult.RC_HISTORY_NOT_PASSED,
                    "HISTORY CHECK MUST BE PASSED BEFORE PAYMENT");
        }

        // 3. Not already paid
        if ("P".equals(app.getPaymentStatus())) {
            return ServiceResult.error(ServiceResult.RC_ALREADY_PAID,
                    "PAYMENT HAS ALREADY BEEN RECORDED FOR THIS APPLICATION");
        }

        // 4. Look up fee schedule
        Optional<BigDecimal> feeOpt = findActiveFee(app.getLicenseType(), "IF");
        if (feeOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_FEE_NOT_FOUND,
                    "NO ACTIVE FEE SCHEDULE FOUND FOR THIS LICENSE TYPE");
        }
        BigDecimal feeAmount = feeOpt.get();

        // 5. Generate receipt number: RCP<yyyyMMdd><applicationId>
        String receiptNumber = "RCP"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + applicationId;

        // 6. Build and persist payment record
        PaymentRecord payment = new PaymentRecord(applicationId, candidateId,
                LocalDate.now(), feeAmount, paymentMethod, paymentReference,
                "S", app.getLicenseType(), "IF", receiptNumber, processedBy);

        long paymentId = insertPayment(payment);
        payment.paymentId = paymentId;

        // 7. Notify application-svc to advance status via REST call
        applicationSvcClient.updatePaymentStatus(applicationId,
                new ApplicationSvcClient.PaymentStatusRequest(
                        "P", paymentReference, "PP",
                        LocalDate.now().toString(), processedBy));

        return ServiceResult.ok(payment, "PAYMENT RECORDED SUCCESSFULLY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // JDBC helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Optional<BigDecimal> findActiveFee(String licenseType, String feeType) {
        String sql = "SELECT FEE_AMOUNT FROM DLIS.LICENSE_FEE_SCHEDULE " +
                "WHERE LICENSE_TYPE=? AND FEE_TYPE=? AND ACTIVE_STATUS='A' " +
                "AND (EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE) " +
                "FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, licenseType);
            ps.setString(2, feeType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBigDecimal("FEE_AMOUNT")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentService.findActiveFee failed", e);
        }
    }

    private long insertPayment(PaymentRecord p) {
        String sql = "INSERT INTO DLIS.PAYMENT " +
                "(APPLICATION_ID,CANDIDATE_ID,PAYMENT_DATE,PAYMENT_AMOUNT," +
                " PAYMENT_METHOD,PAYMENT_REFERENCE,PAYMENT_STATUS,LICENSE_TYPE," +
                " FEE_TYPE,RECEIPT_NUMBER,PROCESSED_BY) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.applicationId);
            ps.setLong(2, p.candidateId);
            ps.setDate(3, Date.valueOf(p.paymentDate));
            ps.setBigDecimal(4, p.paymentAmount);
            ps.setString(5, p.paymentMethod);
            ps.setString(6, p.paymentReference);
            ps.setString(7, p.paymentStatus);
            ps.setString(8, p.licenseType);
            ps.setString(9, p.feeType);
            ps.setString(10, p.receiptNumber);
            ps.setString(11, p.processedBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentService.insertPayment failed", e);
        }
        throw new RuntimeException("PaymentService.insertPayment: no key returned");
    }

    // ── Value object returned from this service ───────────────────────────────

    public static class PaymentRecord {
        public long       paymentId;
        public long       applicationId;
        public long       candidateId;
        public LocalDate  paymentDate;
        public BigDecimal paymentAmount;
        public String     paymentMethod;
        public String     paymentReference;
        public String     paymentStatus;
        public String     licenseType;
        public String     feeType;
        public String     receiptNumber;
        public String     processedBy;

        public PaymentRecord() {}

        public PaymentRecord(long applicationId, long candidateId, LocalDate paymentDate,
                              BigDecimal paymentAmount, String paymentMethod, String paymentReference,
                              String paymentStatus, String licenseType, String feeType,
                              String receiptNumber, String processedBy) {
            this.applicationId   = applicationId;
            this.candidateId     = candidateId;
            this.paymentDate     = paymentDate;
            this.paymentAmount   = paymentAmount;
            this.paymentMethod   = paymentMethod;
            this.paymentReference = paymentReference;
            this.paymentStatus   = paymentStatus;
            this.licenseType     = licenseType;
            this.feeType         = feeType;
            this.receiptNumber   = receiptNumber;
            this.processedBy     = processedBy;
        }
    }
}
