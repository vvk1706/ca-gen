package com.dlis.cics.program;

import com.ibm.cics.server.*;
import com.dlis.cics.commarea.ApplicationCommarea;
import com.dlis.core.domain.LicenseApplication;
import com.dlis.core.service.ApplicationService;
import com.dlis.core.service.ServiceResult;

import javax.inject.Inject;

/**
 * CICS Java Program: DLISAPPL
 * Transaction: APPL
 *
 * Handles application entry (create), eligibility check, history check,
 * and status inquiry.  Dispatched by tran-code sub-function in COMMAREA.
 *
 * Sub-functions (encoded in tranCode field):
 *   APLN — create new application
 *   APEC — perform eligibility check
 *   APHC — perform history check
 *   APIS — inquire application status
 */
public class DlisApplProgram implements Executable {

    @Inject
    private ApplicationService applicationService;

    @Override
    public void execute(CommAreaHolder commArea) throws InvalidRequestException {
        ApplicationCommarea ca = new ApplicationCommarea();
        if (commArea != null && commArea.getValue() != null) {
            ca.fromBytes(commArea.getValue());
        }

        try {
            dispatch(ca);
        } catch (Exception e) {
            ca.setReturnCode(99);
            ca.setReturnMessage(truncate("INTERNAL ERROR: " + e.getMessage(), 100));
        }

        if (commArea != null) {
            commArea.setValue(ca.toBytes());
        }
    }

    private void dispatch(ApplicationCommarea ca) {
        switch (ca.getTranCode() != null ? ca.getTranCode().trim() : "") {
            case "APLN": handleCreate(ca);          break;
            case "APEC": handleEligibility(ca);     break;
            case "APHC": handleHistory(ca);         break;
            case "APIS": handleStatusInquiry(ca);   break;
            default:
                ca.setReturnCode(99);
                ca.setReturnMessage("UNKNOWN FUNCTION CODE: " + ca.getTranCode());
        }
    }

    private void handleCreate(ApplicationCommarea ca) {
        ServiceResult<Long> result = applicationService.createApplication(
                ca.getCandidateId(), ca.getLicenseType(), ca.getUserId());
        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            ca.setApplicationId(result.getPayload());
        }
    }

    private void handleEligibility(ApplicationCommarea ca) {
        ServiceResult<String> result =
                applicationService.checkEligibility(ca.getApplicationId(), ca.getUserId());
        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            ca.setEligibilityStatus(result.getPayload());
        }
    }

    private void handleHistory(ApplicationCommarea ca) {
        ServiceResult<String> result =
                applicationService.checkHistory(ca.getApplicationId(), ca.getUserId());
        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            ca.setHistoryStatus(result.getPayload());
        }
    }

    private void handleStatusInquiry(ApplicationCommarea ca) {
        ServiceResult<LicenseApplication> result =
                applicationService.inquireStatus(ca.getApplicationId());
        ca.setReturnCode(result.getReturnCode());
        ca.setReturnMessage(truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            LicenseApplication app = result.getPayload();
            ca.setCandidateId(app.getCandidateId());
            ca.setLicenseType(app.getLicenseType());
            ca.setApplicationStatus(app.getApplicationStatus());
            ca.setEligibilityStatus(nullSafe(app.getEligibilityChkStatus()));
            ca.setHistoryStatus(nullSafe(app.getHistoryChkStatus()));
            ca.setPaymentStatus(nullSafe(app.getPaymentStatus()));
            ca.setApproval1Status(nullSafe(app.getApproval1Status()));
            ca.setApproval2Status(nullSafe(app.getApproval2Status()));
            ca.setPaymentReference(nullSafe(app.getPaymentReference()));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullSafe(String s) {
        return s == null ? " " : s;
    }
}
