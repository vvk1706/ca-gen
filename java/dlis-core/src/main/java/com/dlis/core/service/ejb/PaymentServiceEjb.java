package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.LicenseFeeScheduleDao;
import com.dlis.core.domain.*;
import com.dlis.core.service.PaymentService;
import com.dlis.core.service.ServiceResult;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * EJB implementation of PaymentService.
 * Logic derived from AB-PROCESS-PAYMENT.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PaymentServiceEjb implements PaymentService {

    @Inject private ApplicationDao      applicationDao;
    @Inject private LicenseFeeScheduleDao feeDao;

    // Package-visible DAO for Payment — using raw JDBC inline for brevity
    @Resource(lookup = "jdbc/dlisDS")
    private javax.sql.DataSource dataSource;

    @Override
    public ServiceResult<Payment> processPayment(Long applicationId,
                                                  Long candidateId,
                                                  String paymentMethod,
                                                  String paymentReference,
                                                  String processedBy) {
        // 1. Load application
        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        // 2. History must be passed
        if (!app.isHistoryPassed()) {
            return ServiceResult.error(ServiceResult.RC_HISTORY_NOT_PASSED,
                    "HISTORY CHECK MUST BE PASSED BEFORE PAYMENT");
        }

        // 3. Not already paid
        if (LicenseApplication.PAYMENT_PAID.equals(app.getPaymentStatus())) {
            return ServiceResult.error(ServiceResult.RC_ALREADY_PAID,
                    "PAYMENT HAS ALREADY BEEN RECORDED FOR THIS APPLICATION");
        }

        // 4. Look up fee
        Optional<LicenseFeeSchedule> feeOpt = feeDao.findActiveFee(app.getLicenseType(), "IF");
        if (feeOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_FEE_NOT_FOUND,
                    "NO ACTIVE FEE SCHEDULE FOUND FOR THIS LICENSE TYPE");
        }
        BigDecimal feeAmount = feeOpt.get().getFeeAmount();

        // 5. Generate receipt number: RCP<yyyyMMdd><applicationId>
        String receiptNumber = "RCP"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + applicationId;

        // 6. Build payment record
        Payment payment = new Payment();
        payment.setApplicationId(applicationId);
        payment.setCandidateId(candidateId);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentAmount(feeAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentReference(paymentReference);
        payment.setPaymentStatus("S");
        payment.setLicenseType(app.getLicenseType());
        payment.setFeeType("IF");
        payment.setReceiptNumber(receiptNumber);
        payment.setProcessedBy(processedBy);

        // 7. Persist payment
        insertPayment(payment);

        // 8. Advance application status
        LocalDate now = LocalDate.now();
        applicationDao.updatePayment(applicationId, "P", paymentReference,
                LicenseApplication.STATUS_PAYMENT_APPROVED, now, processedBy);

        return ServiceResult.ok(payment, "PAYMENT RECORDED SUCCESSFULLY");
    }

    // ---- Inline JDBC insert for Payment (avoids a separate EJB for this small op) ----
    private void insertPayment(Payment p) {
        String sql = "INSERT INTO DLIS.PAYMENT " +
                     "(APPLICATION_ID,CANDIDATE_ID,PAYMENT_DATE,PAYMENT_AMOUNT," +
                     " PAYMENT_METHOD,PAYMENT_REFERENCE,PAYMENT_STATUS,LICENSE_TYPE," +
                     " FEE_TYPE,RECEIPT_NUMBER,PROCESSED_BY) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getApplicationId());
            ps.setLong(2, p.getCandidateId());
            ps.setDate(3, java.sql.Date.valueOf(p.getPaymentDate()));
            ps.setBigDecimal(4, p.getPaymentAmount());
            ps.setString(5, p.getPaymentMethod());
            ps.setString(6, p.getPaymentReference());
            ps.setString(7, p.getPaymentStatus());
            ps.setString(8, p.getLicenseType());
            ps.setString(9, p.getFeeType());
            ps.setString(10, p.getReceiptNumber());
            ps.setString(11, p.getProcessedBy());
            ps.executeUpdate();
            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setPaymentId(keys.getLong(1));
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("PaymentServiceEjb.insertPayment failed", e);
        }
    }
}
