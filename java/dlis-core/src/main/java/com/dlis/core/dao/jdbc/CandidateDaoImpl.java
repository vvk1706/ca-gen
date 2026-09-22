package com.dlis.core.dao.jdbc;

import com.dlis.core.dao.CandidateDao;
import com.dlis.core.domain.Candidate;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

/**
 * JDBC DAO for DLIS.CANDIDATE.
 * DataSource is a DB2 Type 4 connection pool defined in WAS Liberty server.xml.
 */
@Stateless
public class CandidateDaoImpl implements CandidateDao {

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    private static final String INSERT_SQL =
        "INSERT INTO DLIS.CANDIDATE " +
        "(FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,ID_NUMBER,ADDRESS_LINE_1,ADDRESS_LINE_2," +
        " CITY,STATE_PROVINCE,POSTAL_CODE,COUNTRY,PHONE_NUMBER,EMAIL_ADDRESS," +
        " CREATED_DATE,RECORD_STATUS) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SELECT_BY_ID =
        "SELECT CANDIDATE_ID,FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,ID_NUMBER," +
        "  ADDRESS_LINE_1,ADDRESS_LINE_2,CITY,STATE_PROVINCE,POSTAL_CODE,COUNTRY," +
        "  PHONE_NUMBER,EMAIL_ADDRESS,CREATED_DATE,RECORD_STATUS " +
        "FROM DLIS.CANDIDATE WHERE CANDIDATE_ID = ?";

    private static final String SELECT_BY_IDNUM =
        "SELECT CANDIDATE_ID,FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,ID_NUMBER," +
        "  ADDRESS_LINE_1,ADDRESS_LINE_2,CITY,STATE_PROVINCE,POSTAL_CODE,COUNTRY," +
        "  PHONE_NUMBER,EMAIL_ADDRESS,CREATED_DATE,RECORD_STATUS " +
        "FROM DLIS.CANDIDATE WHERE ID_NUMBER = ? AND RECORD_STATUS = 'A' " +
        "FETCH FIRST 1 ROWS ONLY";

    private static final String UPDATE_STATUS =
        "UPDATE DLIS.CANDIDATE SET RECORD_STATUS = ? WHERE CANDIDATE_ID = ?";

    @Override
    public void insert(Candidate c) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL,
                                         Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, c.getFirstName());
            ps.setString(i++, c.getLastName());
            ps.setDate(i++, Date.valueOf(c.getDateOfBirth()));
            ps.setString(i++, c.getIdNumber());
            ps.setString(i++, c.getAddressLine1());
            ps.setString(i++, c.getAddressLine2());
            ps.setString(i++, c.getCity());
            ps.setString(i++, c.getStateProvince());
            ps.setString(i++, c.getPostalCode());
            ps.setString(i++, c.getCountry());
            ps.setString(i++, c.getPhoneNumber());
            ps.setString(i++, c.getEmailAddress());
            ps.setDate(i++, Date.valueOf(LocalDate.now()));
            ps.setString(i,   "A");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setCandidateId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateDao.insert failed", e);
        }
    }

    @Override
    public Optional<Candidate> findById(Long candidateId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateDao.findById failed", e);
        }
    }

    @Override
    public Optional<Candidate> findByIdNumber(String idNumber) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_IDNUM)) {
            ps.setString(1, idNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateDao.findByIdNumber failed", e);
        }
    }

    @Override
    public void updateStatus(Long candidateId, String status) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setLong(2, candidateId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("CandidateDao.updateStatus failed", e);
        }
    }

    // ---- Row mapper ----
    private Candidate mapRow(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setCandidateId(rs.getLong("CANDIDATE_ID"));
        c.setFirstName(rs.getString("FIRST_NAME"));
        c.setLastName(rs.getString("LAST_NAME"));
        c.setDateOfBirth(rs.getDate("DATE_OF_BIRTH").toLocalDate());
        c.setIdNumber(rs.getString("ID_NUMBER"));
        c.setAddressLine1(rs.getString("ADDRESS_LINE_1"));
        c.setAddressLine2(rs.getString("ADDRESS_LINE_2"));
        c.setCity(rs.getString("CITY"));
        c.setStateProvince(rs.getString("STATE_PROVINCE"));
        c.setPostalCode(rs.getString("POSTAL_CODE"));
        c.setCountry(rs.getString("COUNTRY"));
        c.setPhoneNumber(rs.getString("PHONE_NUMBER"));
        c.setEmailAddress(rs.getString("EMAIL_ADDRESS"));
        c.setCreatedDate(rs.getDate("CREATED_DATE").toLocalDate());
        c.setRecordStatus(rs.getString("RECORD_STATUS"));
        return c;
    }
}
