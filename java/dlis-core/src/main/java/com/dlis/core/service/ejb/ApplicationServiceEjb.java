package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.CandidateDao;
import com.dlis.core.dao.DrivingHistoryDao;
import com.dlis.core.dao.IssuedLicenseDao;
import com.dlis.core.domain.*;
import com.dlis.core.service.ApplicationService;
import com.dlis.core.service.ServiceResult;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * EJB implementation of ApplicationService.
 * Directly translates the business logic from:
 *   AB-CREATE-APPLICATION
 *   AB-CHECK-ELIGIBILITY
 *   AB-CHECK-HISTORY
 *   AB-INQUIRE-APPLICATION-STATUS
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ApplicationServiceEjb implements ApplicationService {

    @Inject private ApplicationDao   applicationDao;
    @Inject private CandidateDao     candidateDao;
    @Inject private DrivingHistoryDao historyDao;
    @Inject private IssuedLicenseDao  licenseDao;

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CREATE-APPLICATION
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<Long> createApplication(Long candidateId,
                                                  String licenseType,
                                                  String createdBy) {
        // Validate candidate
        Optional<Candidate> candidateOpt = candidateDao.findById(candidateId);
        if (candidateOpt.isEmpty() || !candidateOpt.get().isActive()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "CANDIDATE NOT FOUND OR INACTIVE");
        }

        // Validate license type
        if (licenseType == null || !licenseType.matches("[LPO]")) {
            return ServiceResult.error(ServiceResult.RC_INVALID_TYPE,
                    "INVALID LICENSE TYPE. MUST BE L, P OR O");
        }

        // Duplicate application guard
        List<LicenseApplication> existing =
                applicationDao.findActiveByCandidate(candidateId, licenseType);
        if (!existing.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_ALREADY_EXISTS,
                    "AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE");
        }

        // Create
        LicenseApplication app = new LicenseApplication();
        app.setCandidateId(candidateId);
        app.setLicenseType(licenseType);
        app.setCreatedBy(createdBy);
        applicationDao.insert(app);

        return ServiceResult.ok(app.getApplicationId(),
                "APPLICATION CREATED SUCCESSFULLY");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CHECK-ELIGIBILITY
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<String> checkEligibility(Long applicationId, String checkedBy) {
        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        Optional<Candidate> candOpt = candidateDao.findById(app.getCandidateId());
        if (candOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_ERROR,
                    "CANDIDATE RECORD NOT FOUND");
        }
        Candidate cand = candOpt.get();

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

        // Upgrade-path check (P requires L, O requires P)
        if (failReason == null && requiredPrevType != null) {
            List<IssuedLicense> prior =
                    licenseDao.findActiveByCandidate(app.getCandidateId(), requiredPrevType);
            if (prior.isEmpty()) {
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

        applicationDao.updateEligibilityCheck(applicationId, eligStatus, now,
                failReason != null ? failReason : "",
                appStatus, now, checkedBy);

        String msg = "P".equals(eligStatus) ? "ELIGIBILITY CHECK PASSED" : failReason;
        return ServiceResult.ok(eligStatus, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-CHECK-HISTORY
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public ServiceResult<String> checkHistory(Long applicationId, String checkedBy) {
        Optional<LicenseApplication> appOpt = applicationDao.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                    "APPLICATION NOT FOUND");
        }
        LicenseApplication app = appOpt.get();

        if (!app.isEligibilityPassed()) {
            return ServiceResult.error(2,
                    "ELIGIBILITY CHECK MUST BE PASSED BEFORE HISTORY CHECK");
        }

        List<DrivingHistory> records = historyDao.findActiveByCandidateId(app.getCandidateId());
        int totalDemerit = 0;
        String failReason = null;

        for (DrivingHistory h : records) {
            // Active suspension / disqualification
            if (h.isActiveSuspensionOrDisqualification() && failReason == null) {
                failReason = "CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION";
            }
            // Accumulate demerit
            if (h.getDemeritPoints() != null) {
                totalDemerit += h.getDemeritPoints();
            }
            // Unpaid fines
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

        applicationDao.updateHistoryCheck(applicationId, histStatus, now,
                failReason != null ? failReason : "",
                appStatus, now, checkedBy);

        String msg = "P".equals(histStatus) ? "HISTORY CHECK PASSED" : failReason;
        return ServiceResult.ok(histStatus, msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AB-INQUIRE-APPLICATION-STATUS
    // ──────────────────────────────────────────────────────────────────────────
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ServiceResult<LicenseApplication> inquireStatus(Long applicationId) {
        return applicationDao.findById(applicationId)
                .map(a -> ServiceResult.ok(a, "OK"))
                .orElse(ServiceResult.error(ServiceResult.RC_NOT_FOUND,
                        "APPLICATION NOT FOUND"));
    }
}
