package com.dlis.approval;

import com.dlis.common.api.ServiceResult;
import com.dlis.common.domain.LicenseApplication;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;

/**
 * Approval and License Issuance Service — Open Liberty CDI bean.
 *
 * Implements CA Gen action blocks:
 *   AB-RECORD-APPROVAL-1
 *   AB-RECORD-APPROVAL-2
 *   AB-ISSUE-LICENSE
 */
@ApplicationScoped
public class ApprovalService {

    @Resource(lookup = "jdbc/dlisDS")
    DataSource dataSource;

    @Inject
    @RestClient
    ApplicationSvcClient applicationSvcClient;

    // ──────────────────────────────────────────────────────────────────────────
    // AB-RECORD-APPROVAL-1
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<Void> recordApproval1(Long applicationId, String authorityUserCode,
                                               String decision, String decisionNotes) {
        if (!"A".equals(decision) && !"R".equals(decision)) {
            return ServiceResult.error(ServiceResult.RC_INVALID_DECISION,
                    "DECISION MUST BE A=APPROVE OR R=REJECT");
        }

        LicenseApplication app = applicationSvcClient.findById(applicationId);
        if (app == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        if (!app.isPaymentComplete()) {
            return ServiceResult.error(ServiceResult.RC_PAYMENT_NOT_DONE,
                    "PAYMENT MUST BE COMPLETED BEFORE FIRST APPROVAL");
        }

        AuthorityUserRow auth = findAuthority(authorityUserCode, "1");
        if (auth == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "AUTHORITY USER NOT FOUND OR NOT ACTIVE AT LEVEL 1");
        }
        if (!auth.isAuthorisedFor(app.getLicenseType())) {
            return ServiceResult.error(ServiceResult.RC_NOT_AUTHORISED,
                    "AUTHORITY USER IS NOT AUTHORISED FOR THIS LICENSE TYPE");
        }

        String newStatus = "A".equals(decision)
                ? LicenseApplication.STATUS_APPROVAL2_PEND
                : LicenseApplication.STATUS_REJECTED;

        applicationSvcClient.updateApproval1(applicationId,
                new ApplicationSvcClient.ApprovalRequest(decision, auth.authorityName,
                        LocalDate.now().toString(), decisionNotes, newStatus,
                        LocalDate.now().toString(), authorityUserCode));

        String msg = "A".equals(decision)
                ? "FIRST APPROVAL RECORDED - APPLICATION FORWARDED FOR SECOND APPROVAL"
                : "FIRST APPROVAL REJECTED - APPLICATION CLOSED";
        return ServiceResult.ok(null, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-RECORD-APPROVAL-2
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<Void> recordApproval2(Long applicationId, String authorityUserCode,
                                               String decision, String decisionNotes) {
        if (!"A".equals(decision) && !"R".equals(decision)) {
            return ServiceResult.error(ServiceResult.RC_INVALID_DECISION,
                    "DECISION MUST BE A=APPROVE OR R=REJECT");
        }

        LicenseApplication app = applicationSvcClient.findById(applicationId);
        if (app == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        if (!LicenseApplication.STATUS_APPROVAL2_PEND.equals(app.getApplicationStatus())) {
            return ServiceResult.error(2,
                    "APPLICATION IS NOT IN A2 STATUS - FIRST APPROVAL NOT YET GRANTED");
        }

        AuthorityUserRow auth = findAuthority(authorityUserCode, "2");
        if (auth == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "AUTHORITY USER NOT FOUND OR NOT ACTIVE AT LEVEL 2");
        }
        if (!auth.isAuthorisedFor(app.getLicenseType())) {
            return ServiceResult.error(ServiceResult.RC_NOT_AUTHORISED,
                    "AUTHORITY USER IS NOT AUTHORISED FOR THIS LICENSE TYPE");
        }

        String newStatus = "A".equals(decision)
                ? LicenseApplication.STATUS_APPROVED
                : LicenseApplication.STATUS_REJECTED;

        applicationSvcClient.updateApproval2(applicationId,
                new ApplicationSvcClient.ApprovalRequest(decision, auth.authorityName,
                        LocalDate.now().toString(), decisionNotes, newStatus,
                        LocalDate.now().toString(), authorityUserCode));

        String msg = "A".equals(decision)
                ? "SECOND APPROVAL GRANTED - APPLICATION FULLY APPROVED AND READY FOR ISSUE"
                : "SECOND APPROVAL REJECTED - APPLICATION CLOSED";
        return ServiceResult.ok(null, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-ISSUE-LICENSE
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<IssuedLicenseRecord> issueLicense(Long applicationId,
                                                            String vehicleClass,
                                                            String restrictions,
                                                            String issuedByOfficer,
                                                            String issuedByAuthority) {
        if (!vehicleClass.matches("[ABCD]")) {
            return ServiceResult.error(3, "VEHICLE CLASS MUST BE A, B, C OR D");
        }

        LicenseApplication app = applicationSvcClient.findById(applicationId);
        if (app == null) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        if (!app.isFullyApproved()) {
            return ServiceResult.error(2,
                    "APPLICATION MUST BE FULLY APPROVED (STATUS=AP) BEFORE LICENSE CAN BE ISSUED");
        }
        if (!app.isEligibilityPassed()) return ServiceResult.error(2, "ELIGIBILITY CHECK HAS NOT BEEN PASSED");
        if (!app.isHistoryPassed())     return ServiceResult.error(2, "HISTORY CHECK HAS NOT BEEN PASSED");
        if (!app.isPaymentComplete())   return ServiceResult.error(2, "PAYMENT HAS NOT BEEN COMPLETED");

        LocalDate issueDate = LocalDate.now();
        int yearsValid;
        String prefix;
        switch (app.getLicenseType()) {
            case "L": yearsValid = 1; prefix = "LRN"; break;
            case "P": yearsValid = 2; prefix = "PRB"; break;
            default:  yearsValid = 5; prefix = "OPN"; break;
        }
        LocalDate expiryDate  = issueDate.plusYears(yearsValid);
        String licenseNumber  = prefix + vehicleClass + app.getCandidateId() + applicationId;

        IssuedLicenseRecord lic = new IssuedLicenseRecord();
        lic.applicationId   = applicationId;
        lic.candidateId     = app.getCandidateId();
        lic.licenseNumber   = licenseNumber;
        lic.licenseType     = app.getLicenseType();
        lic.issueDate       = issueDate;
        lic.expiryDate      = expiryDate;
        lic.licenseStatus   = "A";
        lic.vehicleClass    = vehicleClass;
        lic.restrictions    = restrictions;
        lic.demeritBalance  = 12;
        lic.issuedByAuthority = issuedByAuthority;
        lic.issuedByOfficer   = issuedByOfficer;
        lic.renewalCount    = 0;

        insertIssuedLicense(lic);

        applicationSvcClient.updateApplicationStatus(applicationId,
                new ApplicationSvcClient.StatusRequest(
                        LicenseApplication.STATUS_ISSUED,
                        LocalDate.now().toString(), issuedByOfficer));

        return ServiceResult.ok(lic, "DRIVER LICENSE ISSUED SUCCESSFULLY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // JDBC helpers
    // ──────────────────────────────────────────────────────────────────────────

    private AuthorityUserRow findAuthority(String userCode, String level) {
        String sql = "SELECT USER_CODE,USER_NAME,AUTHORITY_NAME,LICENSE_TYPES_AUTHORISED " +
                "FROM DLIS.AUTHORITY_USER " +
                "WHERE USER_CODE=? AND AUTHORITY_LEVEL=? AND ACTIVE_STATUS='A' FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userCode);
            ps.setString(2, level);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuthorityUserRow a = new AuthorityUserRow();
                    a.userCode                 = rs.getString("USER_CODE");
                    a.authorityName            = rs.getString("AUTHORITY_NAME");
                    a.licenseTypesAuthorised   = rs.getString("LICENSE_TYPES_AUTHORISED");
                    return a;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApprovalService.findAuthority failed", e);
        }
        return null;
    }

    private void insertIssuedLicense(IssuedLicenseRecord lic) {
        String sql = "INSERT INTO DLIS.ISSUED_LICENSE " +
                "(APPLICATION_ID,CANDIDATE_ID,LICENSE_NUMBER,LICENSE_TYPE," +
                " ISSUE_DATE,EXPIRY_DATE,LICENSE_STATUS,VEHICLE_CLASS,RESTRICTIONS," +
                " DEMERIT_BALANCE,ISSUED_BY_AUTHORITY,ISSUED_BY_OFFICER,RENEWAL_COUNT) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   lic.applicationId);
            ps.setLong(2,   lic.candidateId);
            ps.setString(3, lic.licenseNumber);
            ps.setString(4, lic.licenseType);
            ps.setDate(5,   Date.valueOf(lic.issueDate));
            ps.setDate(6,   Date.valueOf(lic.expiryDate));
            ps.setString(7, lic.licenseStatus);
            ps.setString(8, lic.vehicleClass);
            ps.setString(9, lic.restrictions);
            ps.setInt(10,   lic.demeritBalance);
            ps.setString(11, lic.issuedByAuthority);
            ps.setString(12, lic.issuedByOfficer);
            ps.setInt(13,   lic.renewalCount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) lic.licenseId = keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApprovalService.insertIssuedLicense failed", e);
        }
    }

    // ── Inner value objects ───────────────────────────────────────────────────

    static class AuthorityUserRow {
        String userCode;
        String authorityName;
        String licenseTypesAuthorised;

        boolean isAuthorisedFor(String licenseType) {
            return licenseTypesAuthorised != null
                    && licenseTypesAuthorised.contains(licenseType);
        }
    }

    public static class IssuedLicenseRecord {
        public long      licenseId;
        public long      applicationId;
        public long      candidateId;
        public String    licenseNumber;
        public String    licenseType;
        public LocalDate issueDate;
        public LocalDate expiryDate;
        public String    licenseStatus;
        public String    vehicleClass;
        public String    restrictions;
        public int       demeritBalance;
        public String    issuedByAuthority;
        public String    issuedByOfficer;
        public int       renewalCount;
    }
}
