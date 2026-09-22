package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.IssuedLicenseDao;
import com.dlis.core.domain.*;
import com.dlis.core.service.ApprovalService;
import com.dlis.core.service.ServiceResult;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

/**
 * EJB implementation of ApprovalService.
 * Logic derived from AB-RECORD-APPROVAL-1, AB-RECORD-APPROVAL-2, AB-ISSUE-LICENSE.
 *
 * AUTHORITY_USER lookup is done via inline JDBC to avoid a separate EJB
 * while keeping the transactional boundary clean.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ApprovalServiceEjb implements ApprovalService {

    @Inject private ApplicationDao  applicationDao;
    @Inject private IssuedLicenseDao licenseDao;

    @Resource(lookup = "jdbc/dlisDS")
    private javax.sql.DataSource dataSource;

    // ──────────────────────────────────────────────────────────────────────────
    // AB-RECORD-APPROVAL-1
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<Void> recordApproval1(Long applicationId,
                                               String authorityUserCode,
                                               String decision,
                                               String decisionNotes) {
        if (!"A".equals(decision) && !"R".equals(decision)) {
            return ServiceResult.error(ServiceResult.RC_INVALID_DECISION,
                    "DECISION MUST BE A=APPROVE OR R=REJECT");
        }

        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        if (!app.isPaymentComplete()) {
            return ServiceResult.error(ServiceResult.RC_PAYMENT_NOT_DONE,
                    "PAYMENT MUST BE COMPLETED BEFORE FIRST APPROVAL");
        }

        // Validate authority user (level 1)
        AuthorityUser auth = findAuthority(authorityUserCode, "1");
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
        LocalDate now = LocalDate.now();

        applicationDao.updateApproval1(applicationId, decision,
                auth.getAuthorityName(), now, decisionNotes, newStatus, now, authorityUserCode);

        String msg = "A".equals(decision)
                ? "FIRST APPROVAL RECORDED - APPLICATION FORWARDED FOR SECOND APPROVAL"
                : "FIRST APPROVAL REJECTED - APPLICATION CLOSED";
        return ServiceResult.ok(null, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-RECORD-APPROVAL-2
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<Void> recordApproval2(Long applicationId,
                                               String authorityUserCode,
                                               String decision,
                                               String decisionNotes) {
        if (!"A".equals(decision) && !"R".equals(decision)) {
            return ServiceResult.error(ServiceResult.RC_INVALID_DECISION,
                    "DECISION MUST BE A=APPROVE OR R=REJECT");
        }

        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        if (!LicenseApplication.STATUS_APPROVAL2_PEND.equals(app.getApplicationStatus())) {
            return ServiceResult.error(2,
                    "APPLICATION IS NOT IN A2 STATUS - FIRST APPROVAL NOT YET GRANTED");
        }

        AuthorityUser auth = findAuthority(authorityUserCode, "2");
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
        LocalDate now = LocalDate.now();

        applicationDao.updateApproval2(applicationId, decision,
                auth.getAuthorityName(), now, decisionNotes, newStatus, now, authorityUserCode);

        String msg = "A".equals(decision)
                ? "SECOND APPROVAL GRANTED - APPLICATION FULLY APPROVED AND READY FOR ISSUE"
                : "SECOND APPROVAL REJECTED - APPLICATION CLOSED";
        return ServiceResult.ok(null, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-ISSUE-LICENSE
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<IssuedLicense> issueLicense(Long applicationId,
                                                      String vehicleClass,
                                                      String restrictions,
                                                      String issuedByOfficer,
                                                      String issuedByAuthority) {
        if (!vehicleClass.matches("[ABCD]")) {
            return ServiceResult.error(3, "VEHICLE CLASS MUST BE A, B, C OR D");
        }

        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        if (!app.isFullyApproved()) {
            return ServiceResult.error(2,
                    "APPLICATION MUST BE FULLY APPROVED (STATUS=AP) BEFORE LICENSE CAN BE ISSUED");
        }
        if (!app.isEligibilityPassed()) {
            return ServiceResult.error(2, "ELIGIBILITY CHECK HAS NOT BEEN PASSED");
        }
        if (!app.isHistoryPassed()) {
            return ServiceResult.error(2, "HISTORY CHECK HAS NOT BEEN PASSED");
        }
        if (!app.isPaymentComplete()) {
            return ServiceResult.error(2, "PAYMENT HAS NOT BEEN COMPLETED");
        }

        LocalDate issueDate  = LocalDate.now();
        int yearsValid;
        String prefix;
        switch (app.getLicenseType()) {
            case "L": yearsValid = 1; prefix = "LRN"; break;
            case "P": yearsValid = 2; prefix = "PRB"; break;
            default:  yearsValid = 5; prefix = "OPN"; break;  // O
        }
        LocalDate expiryDate = issueDate.plusYears(yearsValid);

        String licenseNumber = prefix + vehicleClass
                + app.getCandidateId()
                + applicationId;

        IssuedLicense lic = new IssuedLicense();
        lic.setApplicationId(applicationId);
        lic.setCandidateId(app.getCandidateId());
        lic.setLicenseNumber(licenseNumber);
        lic.setLicenseType(app.getLicenseType());
        lic.setIssueDate(issueDate);
        lic.setExpiryDate(expiryDate);
        lic.setLicenseStatus(IssuedLicense.STATUS_ACTIVE);
        lic.setVehicleClass(vehicleClass);
        lic.setRestrictions(restrictions);
        lic.setDemeritBalance(12);
        lic.setIssuedByAuthority(issuedByAuthority);
        lic.setIssuedByOfficer(issuedByOfficer);
        lic.setRenewalCount(0);

        licenseDao.insert(lic);

        // Update application to IS
        applicationDao.updateApplicationStatus(applicationId,
                LicenseApplication.STATUS_ISSUED, LocalDate.now(), issuedByOfficer);

        return ServiceResult.ok(lic, "DRIVER LICENSE ISSUED SUCCESSFULLY");
    }

    // ---- Helper: find active authority user with given level ----
    private AuthorityUser findAuthority(String userCode, String level) {
        String sql = "SELECT AUTHORITY_USER_ID,USER_CODE,USER_NAME,AUTHORITY_NAME," +
                     "  AUTHORITY_LEVEL,DEPARTMENT,PHONE_NUMBER,EMAIL_ADDRESS," +
                     "  ACTIVE_STATUS,LICENSE_TYPES_AUTHORISED " +
                     "FROM DLIS.AUTHORITY_USER " +
                     "WHERE USER_CODE=? AND AUTHORITY_LEVEL=? AND ACTIVE_STATUS='A' " +
                     "FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userCode);
            ps.setString(2, level);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuthorityUser a = new AuthorityUser();
                    a.setAuthorityUserId(rs.getLong("AUTHORITY_USER_ID"));
                    a.setUserCode(rs.getString("USER_CODE"));
                    a.setUserName(rs.getString("USER_NAME"));
                    a.setAuthorityName(rs.getString("AUTHORITY_NAME"));
                    a.setAuthorityLevel(rs.getString("AUTHORITY_LEVEL"));
                    a.setDepartment(rs.getString("DEPARTMENT"));
                    a.setPhoneNumber(rs.getString("PHONE_NUMBER"));
                    a.setEmailAddress(rs.getString("EMAIL_ADDRESS"));
                    a.setActiveStatus(rs.getString("ACTIVE_STATUS"));
                    a.setLicenseTypesAuthorised(rs.getString("LICENSE_TYPES_AUTHORISED"));
                    return a;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApprovalServiceEjb.findAuthority failed", e);
        }
        return null;
    }
}
