package com.dlis.application;

import com.dlis.common.api.ServiceResult;
import com.dlis.common.domain.LicenseApplication;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application Lifecycle Service — Open Liberty CDI bean.
 *
 * Implements CA Gen action blocks:
 *   AB-CREATE-APPLICATION
 *   AB-CHECK-ELIGIBILITY
 *   AB-CHECK-HISTORY
 *   AB-INQUIRE-APPLICATION-STATUS
 */
@ApplicationScoped
public class ApplicationService {

    @Resource(lookup = "jdbc/dlisDS")
    DataSource dataSource;

    @Inject
    @RestClient
    CandidateSvcClient candidateSvcClient;

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CREATE-APPLICATION
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<Long> createApplication(Long candidateId,
                                                  String licenseType,
                                                  String createdBy) {
        // Validate candidate via REST call to candidate-svc
        var candidateResult = candidateSvcClient.findById(candidateId);
        if (candidateResult == null || !candidateResult.isActive()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "CANDIDATE NOT FOUND OR INACTIVE");
        }

        // Validate license type
        if (licenseType == null || !licenseType.matches("[LPO]")) {
            return ServiceResult.error(ServiceResult.RC_INVALID_TYPE,
                    "INVALID LICENSE TYPE. MUST BE L, P OR O");
        }

        // Duplicate application guard
        List<LicenseApplication> existing = findActiveByCandidate(candidateId, licenseType);
        if (!existing.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_ALREADY_EXISTS,
                    "AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE");
        }

        // Create
        LicenseApplication app = new LicenseApplication();
        app.setCandidateId(candidateId);
        app.setLicenseType(licenseType);
        app.setCreatedBy(createdBy);
        long id = insertApplication(app);
        app.setApplicationId(id);

        return ServiceResult.ok(id, "APPLICATION CREATED SUCCESSFULLY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CHECK-ELIGIBILITY
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<String> checkEligibility(Long applicationId, String checkedBy) {
        Optional<LicenseApplication> appOpt = findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        var cand = candidateSvcClient.findById(app.getCandidateId());
        if (cand == null) {
            return ServiceResult.error(ServiceResult.RC_ERROR, "CANDIDATE RECORD NOT FOUND");
        }

        int age = Period.between(cand.getDateOfBirth(), LocalDate.now()).getYears();
        String failReason = null;
        String requiredPrevType = null;

        switch (app.getLicenseType()) {
            case "L":
                if (age < 16) failReason = "CANDIDATE MUST BE AT LEAST 16 YEARS OLD FOR LEARNER LICENSE";
                break;
            case "P":
                if (age < 17) failReason = "CANDIDATE MUST BE AT LEAST 17 YEARS OLD FOR PROBATION LICENSE";
                requiredPrevType = "L";
                break;
            case "O":
                if (age < 18) failReason = "CANDIDATE MUST BE AT LEAST 18 YEARS OLD FOR OPEN LICENSE";
                requiredPrevType = "P";
                break;
        }

        if (failReason == null && requiredPrevType != null) {
            boolean hasPrior = hasActiveIssuedLicense(app.getCandidateId(), requiredPrevType);
            if (!hasPrior) {
                failReason = "P".equals(app.getLicenseType())
                    ? "CANDIDATE MUST HOLD AN ACTIVE LEARNER LICENSE TO APPLY FOR PROBATION"
                    : "CANDIDATE MUST HOLD AN ACTIVE PROBATION LICENSE TO APPLY FOR OPEN";
            }
        }

        String eligStatus = failReason == null ? "P" : "F";
        String appStatus  = "P".equals(eligStatus)
                ? LicenseApplication.STATUS_ELIG_CHECKED
                : LicenseApplication.STATUS_REJECTED;
        LocalDate now = LocalDate.now();

        updateEligibilityCheck(applicationId, eligStatus, now,
                failReason != null ? failReason : "", appStatus, now, checkedBy);

        String msg = "P".equals(eligStatus) ? "ELIGIBILITY CHECK PASSED" : failReason;
        return ServiceResult.ok(eligStatus, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CHECK-HISTORY
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<String> checkHistory(Long applicationId, String checkedBy) {
        Optional<LicenseApplication> appOpt = findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        if (!app.isEligibilityPassed()) {
            return ServiceResult.error(2, "ELIGIBILITY CHECK MUST BE PASSED BEFORE HISTORY CHECK");
        }

        // Fetch driving history rows for this candidate
        List<DrivingHistoryRow> records = fetchDrivingHistory(app.getCandidateId());
        int totalDemerit = 0;
        String failReason = null;

        for (DrivingHistoryRow h : records) {
            if (h.isActiveSuspensionOrDisqualification() && failReason == null) {
                failReason = "CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION";
            }
            totalDemerit += h.demeritPoints;
            if (h.hasUnpaidFine() && failReason == null) {
                failReason = "CANDIDATE HAS OUTSTANDING UNPAID FINES";
            }
        }

        if (totalDemerit > 12 && failReason == null) {
            failReason = "TOTAL DEMERIT POINTS EXCEED ALLOWABLE THRESHOLD OF 12";
        }

        String histStatus = failReason == null ? "P" : "F";
        String appStatus  = "P".equals(histStatus)
                ? LicenseApplication.STATUS_HIST_CHECKED
                : LicenseApplication.STATUS_REJECTED;
        LocalDate now = LocalDate.now();

        updateHistoryCheck(applicationId, histStatus, now,
                failReason != null ? failReason : "", appStatus, now, checkedBy);

        String msg = "P".equals(histStatus) ? "HISTORY CHECK PASSED" : failReason;
        return ServiceResult.ok(histStatus, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-INQUIRE-APPLICATION-STATUS
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<LicenseApplication> inquireStatus(Long applicationId) {
        return findById(applicationId)
                .map(a -> ServiceResult.ok(a, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // JDBC helpers
    // ──────────────────────────────────────────────────────────────────────────

    Optional<LicenseApplication> findById(Long id) {
        String sql = "SELECT * FROM DLIS.LICENSE_APPLICATION WHERE APPLICATION_ID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.findById failed", e);
        }
    }

    private List<LicenseApplication> findActiveByCandidate(Long candidateId, String licenseType) {
        String sql = "SELECT * FROM DLIS.LICENSE_APPLICATION " +
                "WHERE CANDIDATE_ID=? AND LICENSE_TYPE=? " +
                "AND APPLICATION_STATUS NOT IN ('RE','IS')";
        List<LicenseApplication> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            ps.setString(2, licenseType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.findActiveByCandidate failed", e);
        }
        return result;
    }

    private boolean hasActiveIssuedLicense(Long candidateId, String licenseType) {
        String sql = "SELECT 1 FROM DLIS.ISSUED_LICENSE " +
                "WHERE CANDIDATE_ID=? AND LICENSE_TYPE=? AND LICENSE_STATUS='A' FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            ps.setString(2, licenseType);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.hasActiveIssuedLicense failed", e);
        }
    }

    private long insertApplication(LicenseApplication app) {
        LocalDate now = LocalDate.now();
        String sql = "INSERT INTO DLIS.LICENSE_APPLICATION " +
                "(CANDIDATE_ID,LICENSE_TYPE,APPLICATION_DATE,APPLICATION_STATUS," +
                " ELIGIBILITY_CHK_STATUS,HISTORY_CHK_STATUS,PAYMENT_STATUS," +
                " APPROVAL_1_STATUS,APPROVAL_2_STATUS," +
                " CREATED_DATE,LAST_UPDATED_DATE,CREATED_BY,LAST_UPDATED_BY) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, app.getCandidateId());
            ps.setString(i++, app.getLicenseType());
            ps.setDate(i++,  Date.valueOf(now));
            ps.setString(i++, LicenseApplication.STATUS_PENDING);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.PAYMENT_UNPAID);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setDate(i++,  Date.valueOf(now));
            ps.setDate(i++,  Date.valueOf(now));
            ps.setString(i++, app.getCreatedBy());
            ps.setString(i,   app.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.insertApplication failed", e);
        }
        throw new RuntimeException("ApplicationService.insertApplication: no key returned");
    }

    private void updateEligibilityCheck(Long applicationId, String status, LocalDate checkDate,
                                        String notes, String appStatus,
                                        LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "ELIGIBILITY_CHK_STATUS=?,ELIGIBILITY_CHK_DATE=?,ELIGIBILITY_CHK_NOTES=?," +
                "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                "WHERE APPLICATION_ID=?";
        executeUpdate(sql, status, Date.valueOf(checkDate), notes,
                appStatus, Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    private void updateHistoryCheck(Long applicationId, String status, LocalDate checkDate,
                                    String notes, String appStatus,
                                    LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "HISTORY_CHK_STATUS=?,HISTORY_CHK_DATE=?,HISTORY_CHK_NOTES=?," +
                "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                "WHERE APPLICATION_ID=?";
        executeUpdate(sql, status, Date.valueOf(checkDate), notes,
                appStatus, Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    public void updatePaymentStatus(Long applicationId, String paymentStatus,
                                    String paymentReference, String appStatus,
                                    LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "PAYMENT_STATUS=?,PAYMENT_REFERENCE=?,APPLICATION_STATUS=?," +
                "LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? WHERE APPLICATION_ID=?";
        executeUpdate(sql, paymentStatus, paymentReference, appStatus,
                Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    public void updateApproval1(Long applicationId, String decision, String authorityName,
                                LocalDate decisionDate, String notes, String appStatus,
                                LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "APPROVAL_1_STATUS=?,APPROVAL_1_AUTHORITY=?,APPROVAL_1_DATE=?,APPROVAL_1_NOTES=?," +
                "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? WHERE APPLICATION_ID=?";
        executeUpdate(sql, decision, authorityName, Date.valueOf(decisionDate), notes,
                appStatus, Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    public void updateApproval2(Long applicationId, String decision, String authorityName,
                                LocalDate decisionDate, String notes, String appStatus,
                                LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "APPROVAL_2_STATUS=?,APPROVAL_2_AUTHORITY=?,APPROVAL_2_DATE=?,APPROVAL_2_NOTES=?," +
                "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? WHERE APPLICATION_ID=?";
        executeUpdate(sql, decision, authorityName, Date.valueOf(decisionDate), notes,
                appStatus, Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    public void updateApplicationStatus(Long applicationId, String appStatus,
                                         LocalDate lastUpdated, String updatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? WHERE APPLICATION_ID=?";
        executeUpdate(sql, appStatus, Date.valueOf(lastUpdated), updatedBy, applicationId);
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.executeUpdate failed: " + sql, e);
        }
    }

    private List<DrivingHistoryRow> fetchDrivingHistory(Long candidateId) {
        String sql = "SELECT INCIDENT_TYPE,DEMERIT_POINTS,FINE_PAID_STATUS," +
                "SUSPENSION_START_DATE,SUSPENSION_END_DATE,RECORD_STATUS " +
                "FROM DLIS.DRIVING_HISTORY WHERE CANDIDATE_ID=? AND RECORD_STATUS='A'";
        List<DrivingHistoryRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DrivingHistoryRow r = new DrivingHistoryRow();
                    r.incidentType   = rs.getString("INCIDENT_TYPE");
                    r.demeritPoints  = rs.getInt("DEMERIT_POINTS");
                    r.finePaidStatus = rs.getString("FINE_PAID_STATUS");
                    Date ss = rs.getDate("SUSPENSION_START_DATE");
                    Date se = rs.getDate("SUSPENSION_END_DATE");
                    r.suspensionStartDate = ss != null ? ss.toLocalDate() : null;
                    r.suspensionEndDate   = se != null ? se.toLocalDate() : null;
                    rows.add(r);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationService.fetchDrivingHistory failed", e);
        }
        return rows;
    }

    private LicenseApplication mapRow(ResultSet rs) throws SQLException {
        LicenseApplication a = new LicenseApplication();
        a.setApplicationId(rs.getLong("APPLICATION_ID"));
        a.setCandidateId(rs.getLong("CANDIDATE_ID"));
        a.setLicenseType(rs.getString("LICENSE_TYPE"));
        Date ad = rs.getDate("APPLICATION_DATE");
        if (ad != null) a.setApplicationDate(ad.toLocalDate());
        a.setApplicationStatus(rs.getString("APPLICATION_STATUS"));
        a.setEligibilityChkStatus(rs.getString("ELIGIBILITY_CHK_STATUS"));
        a.setHistoryChkStatus(rs.getString("HISTORY_CHK_STATUS"));
        a.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        a.setPaymentReference(rs.getString("PAYMENT_REFERENCE"));
        a.setApproval1Status(rs.getString("APPROVAL_1_STATUS"));
        a.setApproval1Authority(rs.getString("APPROVAL_1_AUTHORITY"));
        a.setApproval2Status(rs.getString("APPROVAL_2_STATUS"));
        a.setApproval2Authority(rs.getString("APPROVAL_2_AUTHORITY"));
        a.setCreatedBy(rs.getString("CREATED_BY"));
        a.setLastUpdatedBy(rs.getString("LAST_UPDATED_BY"));
        return a;
    }

    static class DrivingHistoryRow {
        String    incidentType;
        int       demeritPoints;
        String    finePaidStatus;
        LocalDate suspensionStartDate;
        LocalDate suspensionEndDate;

        boolean isActiveSuspensionOrDisqualification() {
            if (!"SU".equals(incidentType) && !"DQ".equals(incidentType)) return false;
            LocalDate today = LocalDate.now();
            return suspensionStartDate != null
                    && !suspensionStartDate.isAfter(today)
                    && (suspensionEndDate == null || !suspensionEndDate.isBefore(today));
        }

        boolean hasUnpaidFine() {
            return "N".equals(finePaidStatus);
        }
    }
}
