package com.dlis.cics.program;

import com.ibm.cics.server.*;
import com.dlis.core.domain.IssuedLicense;
import com.dlis.core.domain.Payment;
import com.dlis.core.service.ApprovalService;
import com.dlis.core.service.PaymentService;
import com.dlis.core.service.ServiceResult;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * CICS Java Program: DLISPAAP
 * Transaction: PAAP  (Payment and Approval)
 *
 * Sub-functions:
 *   PAYM — process payment
 *   APR1 — record approval 1
 *   APR2 — record approval 2
 *   ISSU — issue license
 *
 * COMMAREA layout (320 bytes):
 *   Offset   0:  4  TRAN-CODE / FUNCTION
 *   Offset   4:  4  RETURN-CODE
 *   Offset   8:100  RETURN-MESSAGE
 *   Offset 108: 20  USER-ID
 *   Offset 128: 10  APPLICATION-ID
 *   Offset 138: 10  CANDIDATE-ID
 *   Offset 148:  2  PAYMENT-METHOD
 *   Offset 150: 30  PAYMENT-REFERENCE
 *   Offset 180: 20  RECEIPT-NUMBER
 *   Offset 200: 10  FEE-AMOUNT (numeric, 2 implied decimals)
 *   Offset 210:  1  DECISION       (A/R for approvals)
 *   Offset 211:200  DECISION-NOTES
 *   Offset 411:  2  VEHICLE-CLASS
 *   Offset 413: 20  LICENSE-NUMBER
 *   Offset 433: 10  LICENSE-ID
 *   Offset 443: 50  ISSUED-BY-AUTHORITY
 *   Offset 493: 50  ISSUED-BY-OFFICER
 *   Total = 543 bytes (padded to even)
 */
public class DlisPayApprProgram implements Executable {

    @Inject private PaymentService  paymentService;
    @Inject private ApprovalService approvalService;

    private static final int BUF_SIZE = 544;

    @Override
    public void execute(CommAreaHolder commArea) throws InvalidRequestException {
        byte[] raw = new byte[BUF_SIZE];
        if (commArea != null && commArea.getValue() != null) {
            byte[] in = commArea.getValue();
            System.arraycopy(in, 0, raw, 0, Math.min(in.length, BUF_SIZE));
        }

        String function = decode(raw, 0, 4).trim();
        int    returnCode = 0;
        String returnMsg  = "";

        try {
            switch (function) {
                case "PAYM": returnCode = doPayment(raw);  returnMsg = getMsg(raw);  break;
                case "APR1": returnCode = doApproval1(raw); returnMsg = getMsg(raw); break;
                case "APR2": returnCode = doApproval2(raw); returnMsg = getMsg(raw); break;
                case "ISSU": returnCode = doIssueLicense(raw); returnMsg = getMsg(raw); break;
                default:
                    returnCode = 99;
                    returnMsg = "UNKNOWN FUNCTION: " + function;
                    writeCode(raw, returnCode);
                    writeMsg(raw, returnMsg);
            }
        } catch (Exception e) {
            returnCode = 99;
            returnMsg = truncate("INTERNAL ERROR: " + e.getMessage(), 100);
            writeCode(raw, returnCode);
            writeMsg(raw, returnMsg);
        }

        if (commArea != null) commArea.setValue(raw);
    }

    // ── PAYM ──────────────────────────────────────────────────────────────────
    private int doPayment(byte[] raw) {
        long appId    = decodeLong(raw, 128, 10);
        long candId   = decodeLong(raw, 138, 10);
        String method = decode(raw, 148,  2).trim();
        String ref    = decode(raw, 150, 30).trim();
        String userId = decode(raw, 108, 20).trim();

        ServiceResult<Payment> result =
                paymentService.processPayment(appId, candId, method, ref, userId);

        writeCode(raw, result.getReturnCode());
        writeMsg(raw, truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            encode(raw, 180, result.getPayload().getReceiptNumber(), 20);
            if (result.getPayload().getPaymentAmount() != null) {
                // Store amount as zero-padded integer * 100 (2 implied decimals)
                long amt = result.getPayload().getPaymentAmount()
                        .multiply(java.math.BigDecimal.valueOf(100)).longValue();
                encodeNum(raw, 200, amt, 10);
            }
        }
        return result.getReturnCode();
    }

    // ── APR1 ──────────────────────────────────────────────────────────────────
    private int doApproval1(byte[] raw) {
        long   appId  = decodeLong(raw, 128, 10);
        String auth   = decode(raw, 108, 20).trim();
        String dec    = decode(raw, 210,  1).trim();
        String notes  = decode(raw, 211, 200).trim();

        ServiceResult<Void> result =
                approvalService.recordApproval1(appId, auth, dec, notes);

        writeCode(raw, result.getReturnCode());
        writeMsg(raw, truncate(result.getMessage(), 100));
        return result.getReturnCode();
    }

    // ── APR2 ──────────────────────────────────────────────────────────────────
    private int doApproval2(byte[] raw) {
        long   appId  = decodeLong(raw, 128, 10);
        String auth   = decode(raw, 108, 20).trim();
        String dec    = decode(raw, 210,  1).trim();
        String notes  = decode(raw, 211, 200).trim();

        ServiceResult<Void> result =
                approvalService.recordApproval2(appId, auth, dec, notes);

        writeCode(raw, result.getReturnCode());
        writeMsg(raw, truncate(result.getMessage(), 100));
        return result.getReturnCode();
    }

    // ── ISSU ──────────────────────────────────────────────────────────────────
    private int doIssueLicense(byte[] raw) {
        long   appId     = decodeLong(raw, 128, 10);
        String vehClass  = decode(raw, 411, 2).trim();
        String restrict  = decode(raw, 211, 200).trim();
        String officer   = decode(raw, 493, 50).trim();
        String authority = decode(raw, 443, 50).trim();

        ServiceResult<IssuedLicense> result =
                approvalService.issueLicense(appId, vehClass, restrict, officer, authority);

        writeCode(raw, result.getReturnCode());
        writeMsg(raw, truncate(result.getMessage(), 100));
        if (result.isOk() && result.getPayload() != null) {
            IssuedLicense lic = result.getPayload();
            encode(raw, 413, lic.getLicenseNumber(), 20);
            encodeNum(raw, 433, lic.getLicenseId(), 10);
        }
        return result.getReturnCode();
    }

    // ─── Low-level byte helpers ────────────────────────────────────────────

    private static String decode(byte[] buf, int off, int len) {
        return new String(buf, off, len, StandardCharsets.ISO_8859_1);
    }

    private static long decodeLong(byte[] buf, int off, int len) {
        String s = decode(buf, off, len).trim();
        return s.isEmpty() ? 0L : Long.parseLong(s);
    }

    private static void encode(byte[] buf, int off, String val, int len) {
        byte[] space = new byte[len];
        Arrays.fill(space, (byte) 0x20);
        if (val != null) {
            byte[] src = val.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(src, 0, space, 0, Math.min(src.length, len));
        }
        System.arraycopy(space, 0, buf, off, len);
    }

    private static void encodeNum(byte[] buf, int off, long val, int len) {
        encode(buf, off, String.format("%0" + len + "d", val), len);
    }

    private static void writeCode(byte[] buf, int code) {
        encodeNum(buf, 4, code, 4);
    }

    private static void writeMsg(byte[] buf, String msg) {
        encode(buf, 8, msg, 100);
    }

    private static String getMsg(byte[] buf) {
        return decode(buf, 8, 100).trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
