package com.dlis.candidate;

import com.dlis.common.api.ServiceResult;
import com.dlis.common.domain.Candidate;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Candidate Service — Quarkus CDI bean.
 *
 * Migrated from {@code CandidateServiceEjb} (monolith dlis-core).
 * EJB @Stateless → @ApplicationScoped CDI bean.
 * JTA @TransactionAttribute → Agroal-managed JDBC connections (auto-commit off
 * when called within a Quarkus @Transactional context).
 *
 * Business rules preserved from original CA Gen action blocks:
 *   AB-CREATE-CANDIDATE  (duplicate ID check, DOB in past)
 */
@ApplicationScoped
public class CandidateService {

    @Inject
    DataSource dataSource;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE CANDIDATE  (AB-CREATE-CANDIDATE)
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<Long> createCandidate(Candidate candidate, String createdBy) {

        // Rule 1: Duplicate national ID check
        Optional<Candidate> dup = findByIdNumberInternal(candidate.getIdNumber());
        if (dup.isPresent()) {
            return ServiceResult.error(ServiceResult.RC_DUPLICATE,
                    "CANDIDATE WITH THIS ID NUMBER ALREADY EXISTS");
        }

        // Rule 2: Date of birth must be in the past
        if (candidate.getDateOfBirth() == null
                || !candidate.getDateOfBirth().isBefore(LocalDate.now())) {
            return ServiceResult.error(ServiceResult.RC_INVALID_AGE,
                    "DATE OF BIRTH MUST BE IN THE PAST");
        }

        // Set defaults
        candidate.setCreatedDate(LocalDate.now());
        candidate.setRecordStatus("A");

        // Persist
        long candidateId = insertCandidate(candidate, createdBy);
        candidate.setCandidateId(candidateId);

        return ServiceResult.ok(candidateId, "CANDIDATE CREATED SUCCESSFULLY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // QUERIES
    // ──────────────────────────────────────────────────────────────────────────

    public ServiceResult<Candidate> findByIdNumber(String idNumber) {
        return findByIdNumberInternal(idNumber)
                .map(c -> ServiceResult.ok(c, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "CANDIDATE NOT FOUND"));
    }

    public ServiceResult<Candidate> findById(Long candidateId) {
        return findByIdInternal(candidateId)
                .map(c -> ServiceResult.ok(c, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "CANDIDATE NOT FOUND"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // JDBC helpers
    // ──────────────────────────────────────────────────────────────────────────

    private long insertCandidate(Candidate c, String createdBy) {
        String sql = "INSERT INTO DLIS.CANDIDATE " +
                "(FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,ID_NUMBER," +
                " ADDRESS_LINE_1,ADDRESS_LINE_2,CITY,STATE_PROVINCE," +
                " POSTAL_CODE,COUNTRY,PHONE_NUMBER,EMAIL_ADDRESS," +
                " CREATED_DATE,RECORD_STATUS) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, c.getFirstName());
            ps.setString(i++, c.getLastName());
            ps.setDate(i++,   Date.valueOf(c.getDateOfBirth()));
            ps.setString(i++, c.getIdNumber());
            ps.setString(i++, c.getAddressLine1());
            ps.setString(i++, c.getAddressLine2());
            ps.setString(i++, c.getCity());
            ps.setString(i++, c.getStateProvince());
            ps.setString(i++, c.getPostalCode());
            ps.setString(i++, c.getCountry());
            ps.setString(i++, c.getPhoneNumber());
            ps.setString(i++, c.getEmailAddress());
            ps.setDate(i++,   Date.valueOf(LocalDate.now()));
            ps.setString(i,   "A");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateService.insertCandidate failed", e);
        }
        throw new RuntimeException("CandidateService.insertCandidate: no key returned");
    }

    private Optional<Candidate> findByIdNumberInternal(String idNumber) {
        String sql = "SELECT * FROM DLIS.CANDIDATE WHERE ID_NUMBER = ? FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateService.findByIdNumber failed", e);
        }
    }

    private Optional<Candidate> findByIdInternal(Long candidateId) {
        String sql = "SELECT * FROM DLIS.CANDIDATE WHERE CANDIDATE_ID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("CandidateService.findById failed", e);
        }
    }

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
        Date cd = rs.getDate("CREATED_DATE");
        if (cd != null) c.setCreatedDate(cd.toLocalDate());
        c.setRecordStatus(rs.getString("RECORD_STATUS"));
        return c;
    }
}
