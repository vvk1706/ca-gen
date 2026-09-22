package com.dlis.core.dao.jdbc;

import com.dlis.core.dao.DrivingHistoryDao;
import com.dlis.core.dao.IssuedLicenseDao;
import com.dlis.core.dao.LicenseFeeScheduleDao;
import com.dlis.core.domain.*;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ── DrivingHistoryDaoImpl ─────────────────────────────────────────────────────

@Stateless
class DrivingHistoryDaoImpl implements DrivingHistoryDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String SELECT_ACTIVE =
        "SELECT HISTORY_ID,CANDIDATE_ID,INCIDENT_DATE,INCIDENT_TYPE," +
        "  INCIDENT_DESCRIPTION,DEMERIT_POINTS,FINE_AMOUNT,FINE_PAID_STATUS," +
        "  SUSPENSION_START_DATE,SUSPENSION_END_DATE,COURT_CASE_NUMBER," +
        "  RECORDED_BY_AUTHORITY,RECORD_STATUS " +
        "FROM DLIS.DRIVING_HISTORY " +
        "WHERE CANDIDATE_ID = ? AND RECORD_STATUS = 'A'";

    @Override
    public List<DrivingHistory> findActiveByCandidateId(Long candidateId) {
        List<DrivingHistory> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE)) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DrivingHistory h = new DrivingHistory();
                    h.setHistoryId(rs.getLong("HISTORY_ID"));
                    h.setCandidateId(rs.getLong("CANDIDATE_ID"));
                    h.setIncidentDate(rs.getDate("INCIDENT_DATE").toLocalDate());
                    h.setIncidentType(rs.getString("INCIDENT_TYPE"));
                    h.setIncidentDescription(rs.getString("INCIDENT_DESCRIPTION"));
                    h.setDemeritPoints(rs.getInt("DEMERIT_POINTS"));
                    BigDecimal fa = rs.getBigDecimal("FINE_AMOUNT");
                    h.setFineAmount(rs.wasNull() ? BigDecimal.ZERO : fa);
                    h.setFinePaidStatus(rs.getString("FINE_PAID_STATUS"));
                    Date ssd = rs.getDate("SUSPENSION_START_DATE");
                    h.setSuspensionStartDate(ssd != null ? ssd.toLocalDate() : null);
                    Date sed = rs.getDate("SUSPENSION_END_DATE");
                    h.setSuspensionEndDate(sed != null ? sed.toLocalDate() : null);
                    h.setCourtCaseNumber(rs.getString("COURT_CASE_NUMBER"));
                    h.setRecordedByAuthority(rs.getString("RECORDED_BY_AUTHORITY"));
                    h.setRecordStatus(rs.getString("RECORD_STATUS"));
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DrivingHistoryDao.findActiveByCandidateId failed", e);
        }
        return list;
    }
}

// ── IssuedLicenseDaoImpl ──────────────────────────────────────────────────────

@Stateless
class IssuedLicenseDaoImpl implements IssuedLicenseDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String INSERT_SQL =
        "INSERT INTO DLIS.ISSUED_LICENSE " +
        "(APPLICATION_ID,CANDIDATE_ID,LICENSE_NUMBER,LICENSE_TYPE," +
        " ISSUE_DATE,EXPIRY_DATE,LICENSE_STATUS,VEHICLE_CLASS,RESTRICTIONS," +
        " DEMERIT_BALANCE,ISSUED_BY_AUTHORITY,ISSUED_BY_OFFICER,RENEWAL_COUNT) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SELECT_BY_ID =
        "SELECT * FROM DLIS.ISSUED_LICENSE WHERE LICENSE_ID = ?";

    private static final String SELECT_ACTIVE_BY_CAND =
        "SELECT * FROM DLIS.ISSUED_LICENSE " +
        "WHERE CANDIDATE_ID=? AND LICENSE_TYPE=? AND LICENSE_STATUS='A'";

    @Override
    public void insert(IssuedLicense lic) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL,
                                         Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, lic.getApplicationId());
            ps.setLong(i++, lic.getCandidateId());
            ps.setString(i++, lic.getLicenseNumber());
            ps.setString(i++, lic.getLicenseType());
            ps.setDate(i++, Date.valueOf(lic.getIssueDate()));
            ps.setDate(i++, Date.valueOf(lic.getExpiryDate()));
            ps.setString(i++, lic.getLicenseStatus());
            ps.setString(i++, lic.getVehicleClass());
            ps.setString(i++, lic.getRestrictions());
            ps.setInt(i++, lic.getDemeritBalance() != null ? lic.getDemeritBalance() : 12);
            ps.setString(i++, lic.getIssuedByAuthority());
            ps.setString(i++, lic.getIssuedByOfficer());
            ps.setInt(i, lic.getRenewalCount() != null ? lic.getRenewalCount() : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    lic.setLicenseId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("IssuedLicenseDao.insert failed", e);
        }
    }

    @Override
    public Optional<IssuedLicense> findById(Long licenseId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, licenseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("IssuedLicenseDao.findById failed", e);
        }
    }

    @Override
    public Optional<IssuedLicense> findByLicenseNumber(String number) {
        String sql = "SELECT * FROM DLIS.ISSUED_LICENSE WHERE LICENSE_NUMBER=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("IssuedLicenseDao.findByLicenseNumber failed", e);
        }
    }

    @Override
    public List<IssuedLicense> findActiveByCandidate(Long candidateId, String licenseType) {
        List<IssuedLicense> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_BY_CAND)) {
            ps.setLong(1, candidateId);
            ps.setString(2, licenseType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("IssuedLicenseDao.findActiveByCandidate failed", e);
        }
        return list;
    }

    private IssuedLicense mapRow(ResultSet rs) throws SQLException {
        IssuedLicense l = new IssuedLicense();
        l.setLicenseId(rs.getLong("LICENSE_ID"));
        l.setApplicationId(rs.getLong("APPLICATION_ID"));
        l.setCandidateId(rs.getLong("CANDIDATE_ID"));
        l.setLicenseNumber(rs.getString("LICENSE_NUMBER"));
        l.setLicenseType(rs.getString("LICENSE_TYPE"));
        l.setIssueDate(rs.getDate("ISSUE_DATE").toLocalDate());
        l.setExpiryDate(rs.getDate("EXPIRY_DATE").toLocalDate());
        l.setLicenseStatus(rs.getString("LICENSE_STATUS"));
        l.setVehicleClass(rs.getString("VEHICLE_CLASS"));
        l.setRestrictions(rs.getString("RESTRICTIONS"));
        l.setDemeritBalance(rs.getInt("DEMERIT_BALANCE"));
        l.setIssuedByAuthority(rs.getString("ISSUED_BY_AUTHORITY"));
        l.setIssuedByOfficer(rs.getString("ISSUED_BY_OFFICER"));
        l.setRenewalCount(rs.getInt("RENEWAL_COUNT"));
        long prev = rs.getLong("PREVIOUS_LICENSE_ID");
        l.setPreviousLicenseId(rs.wasNull() ? null : prev);
        l.setNotes(rs.getString("NOTES"));
        return l;
    }
}

// ── LicenseFeeScheduleDaoImpl ─────────────────────────────────────────────────

@Stateless
class LicenseFeeScheduleDaoImpl implements LicenseFeeScheduleDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String SELECT_ACTIVE_FEE =
        "SELECT FEE_SCHEDULE_ID,LICENSE_TYPE,FEE_TYPE,FEE_AMOUNT," +
        "  EFFECTIVE_DATE,EXPIRY_DATE,CURRENCY_CODE,DESCRIPTION,ACTIVE_STATUS " +
        "FROM DLIS.LICENSE_FEE_SCHEDULE " +
        "WHERE LICENSE_TYPE=? AND FEE_TYPE=? AND ACTIVE_STATUS='A' " +
        "  AND EFFECTIVE_DATE <= CURRENT DATE " +
        "  AND (EXPIRY_DATE IS NULL OR EXPIRY_DATE >= CURRENT DATE) " +
        "FETCH FIRST 1 ROWS ONLY";

    @Override
    public Optional<LicenseFeeSchedule> findActiveFee(String licenseType, String feeType) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_FEE)) {
            ps.setString(1, licenseType);
            ps.setString(2, feeType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LicenseFeeSchedule f = new LicenseFeeSchedule();
                    f.setFeeScheduleId(rs.getLong("FEE_SCHEDULE_ID"));
                    f.setLicenseType(rs.getString("LICENSE_TYPE"));
                    f.setFeeType(rs.getString("FEE_TYPE"));
                    f.setFeeAmount(rs.getBigDecimal("FEE_AMOUNT"));
                    f.setEffectiveDate(rs.getDate("EFFECTIVE_DATE").toLocalDate());
                    Date ed = rs.getDate("EXPIRY_DATE");
                    f.setExpiryDate(ed != null ? ed.toLocalDate() : null);
                    f.setCurrencyCode(rs.getString("CURRENCY_CODE"));
                    f.setDescription(rs.getString("DESCRIPTION"));
                    f.setActiveStatus(rs.getString("ACTIVE_STATUS"));
                    return Optional.of(f);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("LicenseFeeScheduleDao.findActiveFee failed", e);
        }
        return Optional.empty();
    }
}
