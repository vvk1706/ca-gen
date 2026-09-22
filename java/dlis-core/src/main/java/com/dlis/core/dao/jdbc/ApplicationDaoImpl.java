package com.dlis.core.dao.jdbc;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.domain.LicenseApplication;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for DLIS.LICENSE_APPLICATION.
 */
@Stateless
public class ApplicationDaoImpl implements ApplicationDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String INSERT_SQL =
        "INSERT INTO DLIS.LICENSE_APPLICATION " +
        "(CANDIDATE_ID,LICENSE_TYPE,APPLICATION_DATE,APPLICATION_STATUS," +
        " ELIGIBILITY_CHK_STATUS,HISTORY_CHK_STATUS,PAYMENT_STATUS," +
        " APPROVAL_1_STATUS,APPROVAL_2_STATUS," +
        " CREATED_DATE,LAST_UPDATED_DATE,CREATED_BY,LAST_UPDATED_BY) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SELECT_BY_ID =
        "SELECT * FROM DLIS.LICENSE_APPLICATION WHERE APPLICATION_ID = ?";

    private static final String SELECT_ACTIVE_BY_CAND =
        "SELECT * FROM DLIS.LICENSE_APPLICATION " +
        "WHERE CANDIDATE_ID = ? AND LICENSE_TYPE = ? " +
        "AND APPLICATION_STATUS NOT IN ('RE','IS')";

    @Override
    public void insert(LicenseApplication app) {
        LocalDate now = LocalDate.now();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL,
                                         Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, app.getCandidateId());
            ps.setString(i++, app.getLicenseType());
            ps.setDate(i++, Date.valueOf(now));
            ps.setString(i++, LicenseApplication.STATUS_PENDING);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.PAYMENT_UNPAID);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setString(i++, LicenseApplication.CHECK_UNCHECKED);
            ps.setDate(i++, Date.valueOf(now));
            ps.setDate(i++, Date.valueOf(now));
            ps.setString(i++, app.getCreatedBy());
            ps.setString(i,   app.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    app.setApplicationId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationDao.insert failed", e);
        }
    }

    @Override
    public Optional<LicenseApplication> findById(Long applicationId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationDao.findById failed", e);
        }
    }

    @Override
    public List<LicenseApplication> findActiveByCandidate(Long candidateId, String licenseType) {
        List<LicenseApplication> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_BY_CAND)) {
            ps.setLong(1, candidateId);
            ps.setString(2, licenseType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationDao.findActiveByCandidate failed", e);
        }
        return result;
    }

    @Override
    public void updateEligibilityCheck(Long applicationId, String status, LocalDate checkDate,
                                       String notes, String appStatus,
                                       LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "ELIGIBILITY_CHK_STATUS=?,ELIGIBILITY_CHK_DATE=?,ELIGIBILITY_CHK_NOTES=?," +
                     "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, status, Date.valueOf(checkDate), notes,
                      appStatus, Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    @Override
    public void updateHistoryCheck(Long applicationId, String status, LocalDate checkDate,
                                   String notes, String appStatus,
                                   LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "HISTORY_CHK_STATUS=?,HISTORY_CHK_DATE=?,HISTORY_CHK_NOTES=?," +
                     "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, status, Date.valueOf(checkDate), notes,
                      appStatus, Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    @Override
    public void updatePayment(Long applicationId, String paymentStatus, String paymentReference,
                              String appStatus, LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "PAYMENT_STATUS=?,PAYMENT_REFERENCE=?,APPLICATION_STATUS=?," +
                     "LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, paymentStatus, paymentReference, appStatus,
                      Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    @Override
    public void updateApproval1(Long applicationId, String decision, String authorityName,
                                LocalDate decisionDate, String notes, String appStatus,
                                LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "APPROVAL_1_STATUS=?,APPROVAL_1_AUTHORITY=?,APPROVAL_1_DATE=?,APPROVAL_1_NOTES=?," +
                     "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, decision, authorityName, Date.valueOf(decisionDate), notes,
                      appStatus, Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    @Override
    public void updateApproval2(Long applicationId, String decision, String authorityName,
                                LocalDate decisionDate, String notes, String appStatus,
                                LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "APPROVAL_2_STATUS=?,APPROVAL_2_AUTHORITY=?,APPROVAL_2_DATE=?,APPROVAL_2_NOTES=?," +
                     "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, decision, authorityName, Date.valueOf(decisionDate), notes,
                      appStatus, Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    @Override
    public void updateApplicationStatus(Long applicationId, String appStatus,
                                        LocalDate lastUpdatedDate, String lastUpdatedBy) {
        String sql = "UPDATE DLIS.LICENSE_APPLICATION SET " +
                     "APPLICATION_STATUS=?,LAST_UPDATED_DATE=?,LAST_UPDATED_BY=? " +
                     "WHERE APPLICATION_ID=?";
        executeUpdate(sql, appStatus, Date.valueOf(lastUpdatedDate), lastUpdatedBy, applicationId);
    }

    // ---- Helper ----

    private void executeUpdate(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ApplicationDao update failed: " + sql, e);
        }
    }

    // ---- Row mapper ----
    private LicenseApplication mapRow(ResultSet rs) throws SQLException {
        LicenseApplication a = new LicenseApplication();
        a.setApplicationId(rs.getLong("APPLICATION_ID"));
        a.setCandidateId(rs.getLong("CANDIDATE_ID"));
        a.setLicenseType(rs.getString("LICENSE_TYPE"));
        a.setApplicationDate(toLD(rs.getDate("APPLICATION_DATE")));
        a.setApplicationStatus(rs.getString("APPLICATION_STATUS"));
        a.setEligibilityChkStatus(rs.getString("ELIGIBILITY_CHK_STATUS"));
        a.setEligibilityChkDate(toLD(rs.getDate("ELIGIBILITY_CHK_DATE")));
        a.setEligibilityChkNotes(rs.getString("ELIGIBILITY_CHK_NOTES"));
        a.setHistoryChkStatus(rs.getString("HISTORY_CHK_STATUS"));
        a.setHistoryChkDate(toLD(rs.getDate("HISTORY_CHK_DATE")));
        a.setHistoryChkNotes(rs.getString("HISTORY_CHK_NOTES"));
        a.setPaymentStatus(rs.getString("PAYMENT_STATUS"));
        a.setPaymentReference(rs.getString("PAYMENT_REFERENCE"));
        a.setApproval1Status(rs.getString("APPROVAL_1_STATUS"));
        a.setApproval1Authority(rs.getString("APPROVAL_1_AUTHORITY"));
        a.setApproval1Date(toLD(rs.getDate("APPROVAL_1_DATE")));
        a.setApproval1Notes(rs.getString("APPROVAL_1_NOTES"));
        a.setApproval2Status(rs.getString("APPROVAL_2_STATUS"));
        a.setApproval2Authority(rs.getString("APPROVAL_2_AUTHORITY"));
        a.setApproval2Date(toLD(rs.getDate("APPROVAL_2_DATE")));
        a.setApproval2Notes(rs.getString("APPROVAL_2_NOTES"));
        a.setRejectionReason(rs.getString("REJECTION_REASON"));
        a.setCreatedDate(toLD(rs.getDate("CREATED_DATE")));
        a.setLastUpdatedDate(toLD(rs.getDate("LAST_UPDATED_DATE")));
        a.setCreatedBy(rs.getString("CREATED_BY"));
        a.setLastUpdatedBy(rs.getString("LAST_UPDATED_BY"));
        return a;
    }

    private LocalDate toLD(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
