package com.dlis.cics.commarea;

/**
 * COMMAREA for DLISAPPL — Application Entry / Status transaction.
 *
 * Layout (total: 180 bytes):
 *   Offset   0:  4  TRAN-CODE
 *   Offset   4:  4  RETURN-CODE
 *   Offset   8:100  RETURN-MESSAGE
 *   Offset 108: 20  USER-ID
 *   Offset 128: 10  APPLICATION-ID (numeric)
 *   Offset 138: 10  CANDIDATE-ID (numeric)
 *   Offset 148:  1  LICENSE-TYPE
 *   Offset 149:  2  APPLICATION-STATUS
 *   Offset 151:  1  ELIGIBILITY-STATUS
 *   Offset 152:  1  HISTORY-STATUS
 *   Offset 153:  1  PAYMENT-STATUS
 *   Offset 154:  1  APPROVAL-1-STATUS
 *   Offset 155:  1  APPROVAL-2-STATUS
 *   Offset 156: 20  PAYMENT-REFERENCE
 *   Offset 176:  4  padding
 *   Total = 180 bytes
 */
public class ApplicationCommarea extends BaseCommarea {

    private long   applicationId;
    private long   candidateId;
    private String licenseType;
    private String applicationStatus;
    private String eligibilityStatus;
    private String historyStatus;
    private String paymentStatus;
    private String approval1Status;
    private String approval2Status;
    private String paymentReference;

    public ApplicationCommarea() {
        this.tranCode = "APPL";
    }

    @Override
    public byte[] toBytes() {
        byte[] buf = new byte[180];
        int off = 0;
        copy(buf, off, encodeString(tranCode,          4)); off +=   4;
        copy(buf, off, encodeNumeric(returnCode,       4)); off +=   4;
        copy(buf, off, encodeString(returnMessage,   100)); off += 100;
        copy(buf, off, encodeString(userId,           20)); off +=  20;
        copy(buf, off, encodeNumeric(applicationId,   10)); off +=  10;
        copy(buf, off, encodeNumeric(candidateId,     10)); off +=  10;
        copy(buf, off, encodeString(licenseType,       1)); off +=   1;
        copy(buf, off, encodeString(applicationStatus, 2)); off +=   2;
        copy(buf, off, encodeString(eligibilityStatus, 1)); off +=   1;
        copy(buf, off, encodeString(historyStatus,     1)); off +=   1;
        copy(buf, off, encodeString(paymentStatus,     1)); off +=   1;
        copy(buf, off, encodeString(approval1Status,   1)); off +=   1;
        copy(buf, off, encodeString(approval2Status,   1)); off +=   1;
        copy(buf, off, encodeString(paymentReference, 20));
        return buf;
    }

    @Override
    public void fromBytes(byte[] data) {
        int off = 0;
        tranCode          = decodeString (data, off,   4); off +=   4;
        returnCode        = (int) decodeNumeric(data, off,   4); off +=   4;
        returnMessage     = decodeString (data, off, 100); off += 100;
        userId            = decodeString (data, off,  20); off +=  20;
        applicationId     = decodeNumeric(data, off,  10); off +=  10;
        candidateId       = decodeNumeric(data, off,  10); off +=  10;
        licenseType       = decodeString (data, off,   1); off +=   1;
        applicationStatus = decodeString (data, off,   2); off +=   2;
        eligibilityStatus = decodeString (data, off,   1); off +=   1;
        historyStatus     = decodeString (data, off,   1); off +=   1;
        paymentStatus     = decodeString (data, off,   1); off +=   1;
        approval1Status   = decodeString (data, off,   1); off +=   1;
        approval2Status   = decodeString (data, off,   1); off +=   1;
        paymentReference  = decodeString (data, off,  20);
    }

    private static void copy(byte[] dest, int offset, byte[] src) {
        System.arraycopy(src, 0, dest, offset, src.length);
    }

    public long   getApplicationId()                    { return applicationId; }
    public void   setApplicationId(long v)              { this.applicationId = v; }
    public long   getCandidateId()                      { return candidateId; }
    public void   setCandidateId(long v)                { this.candidateId = v; }
    public String getLicenseType()                      { return licenseType; }
    public void   setLicenseType(String v)              { this.licenseType = v; }
    public String getApplicationStatus()                { return applicationStatus; }
    public void   setApplicationStatus(String v)        { this.applicationStatus = v; }
    public String getEligibilityStatus()                { return eligibilityStatus; }
    public void   setEligibilityStatus(String v)        { this.eligibilityStatus = v; }
    public String getHistoryStatus()                    { return historyStatus; }
    public void   setHistoryStatus(String v)            { this.historyStatus = v; }
    public String getPaymentStatus()                    { return paymentStatus; }
    public void   setPaymentStatus(String v)            { this.paymentStatus = v; }
    public String getApproval1Status()                  { return approval1Status; }
    public void   setApproval1Status(String v)          { this.approval1Status = v; }
    public String getApproval2Status()                  { return approval2Status; }
    public void   setApproval2Status(String v)          { this.approval2Status = v; }
    public String getPaymentReference()                 { return paymentReference; }
    public void   setPaymentReference(String v)         { this.paymentReference = v; }
}
