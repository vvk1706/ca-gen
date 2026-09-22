package com.dlis.cics.commarea;

/**
 * COMMAREA for DLISCAND — Candidate Maintenance transaction.
 *
 * Layout (total: 396 bytes):
 *   Offset  0 :  4  bytes  TRAN-CODE
 *   Offset  4 :  4  bytes  RETURN-CODE (numeric)
 *   Offset  8 :100  bytes  RETURN-MESSAGE
 *   Offset108 : 20  bytes  USER-ID
 *   Offset128 : 10  bytes  CANDIDATE-ID (numeric)
 *   Offset138 : 30  bytes  FIRST-NAME
 *   Offset168 : 30  bytes  LAST-NAME
 *   Offset198 : 10  bytes  DATE-OF-BIRTH  (CCYYMMDD)
 *   Offset208 : 20  bytes  ID-NUMBER
 *   Offset228 : 50  bytes  ADDRESS-LINE-1
 *   Offset278 : 50  bytes  ADDRESS-LINE-2
 *   Offset328 : 30  bytes  CITY
 *   Offset358 : 15  bytes  PHONE-NUMBER
 *   Offset373 : 20  bytes  EMAIL-ADDRESS (truncated to 20 in COMMAREA)
 *   Offset393 :  1  byte   RECORD-STATUS
 *   Offset394 :  2  bytes  padding
 *   Total    = 396 bytes
 */
public class CandidateCommarea extends BaseCommarea {

    private long   candidateId;
    private String firstName;
    private String lastName;
    private String dateOfBirth;   // CCYYMMDD string
    private String idNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String phoneNumber;
    private String emailAddress;
    private String recordStatus;

    public CandidateCommarea() {
        this.tranCode = "CAND";
    }

    @Override
    public byte[] toBytes() {
        byte[] buf = new byte[396];
        int off = 0;
        copy(buf, off, encodeString(tranCode,       4));    off +=   4;
        copy(buf, off, encodeNumeric(returnCode,    4));    off +=   4;
        copy(buf, off, encodeString(returnMessage, 100));   off += 100;
        copy(buf, off, encodeString(userId,         20));   off +=  20;
        copy(buf, off, encodeNumeric(candidateId,   10));   off +=  10;
        copy(buf, off, encodeString(firstName,      30));   off +=  30;
        copy(buf, off, encodeString(lastName,       30));   off +=  30;
        copy(buf, off, encodeString(dateOfBirth,    10));   off +=  10;
        copy(buf, off, encodeString(idNumber,       20));   off +=  20;
        copy(buf, off, encodeString(addressLine1,   50));   off +=  50;
        copy(buf, off, encodeString(addressLine2,   50));   off +=  50;
        copy(buf, off, encodeString(city,           30));   off +=  30;
        copy(buf, off, encodeString(phoneNumber,    15));   off +=  15;
        copy(buf, off, encodeString(emailAddress,   20));   off +=  20;
        copy(buf, off, encodeString(recordStatus,    1));
        return buf;
    }

    @Override
    public void fromBytes(byte[] data) {
        int off = 0;
        tranCode      = decodeString(data, off,   4);  off +=   4;
        returnCode    = (int) decodeNumeric(data, off,   4);  off +=   4;
        returnMessage = decodeString(data, off, 100);  off += 100;
        userId        = decodeString(data, off,  20);  off +=  20;
        candidateId   = decodeNumeric(data, off,  10);  off +=  10;
        firstName     = decodeString(data, off,  30);  off +=  30;
        lastName      = decodeString(data, off,  30);  off +=  30;
        dateOfBirth   = decodeString(data, off,  10);  off +=  10;
        idNumber      = decodeString(data, off,  20);  off +=  20;
        addressLine1  = decodeString(data, off,  50);  off +=  50;
        addressLine2  = decodeString(data, off,  50);  off +=  50;
        city          = decodeString(data, off,  30);  off +=  30;
        phoneNumber   = decodeString(data, off,  15);  off +=  15;
        emailAddress  = decodeString(data, off,  20);  off +=  20;
        recordStatus  = decodeString(data, off,   1);
    }

    private static void copy(byte[] dest, int offset, byte[] src) {
        System.arraycopy(src, 0, dest, offset, src.length);
    }

    // ---- Getters / Setters ----

    public long   getCandidateId()                  { return candidateId; }
    public void   setCandidateId(long v)            { this.candidateId = v; }
    public String getFirstName()                    { return firstName; }
    public void   setFirstName(String v)            { this.firstName = v; }
    public String getLastName()                     { return lastName; }
    public void   setLastName(String v)             { this.lastName = v; }
    public String getDateOfBirth()                  { return dateOfBirth; }
    public void   setDateOfBirth(String v)          { this.dateOfBirth = v; }
    public String getIdNumber()                     { return idNumber; }
    public void   setIdNumber(String v)             { this.idNumber = v; }
    public String getAddressLine1()                 { return addressLine1; }
    public void   setAddressLine1(String v)         { this.addressLine1 = v; }
    public String getAddressLine2()                 { return addressLine2; }
    public void   setAddressLine2(String v)         { this.addressLine2 = v; }
    public String getCity()                         { return city; }
    public void   setCity(String v)                 { this.city = v; }
    public String getPhoneNumber()                  { return phoneNumber; }
    public void   setPhoneNumber(String v)          { this.phoneNumber = v; }
    public String getEmailAddress()                 { return emailAddress; }
    public void   setEmailAddress(String v)         { this.emailAddress = v; }
    public String getRecordStatus()                 { return recordStatus; }
    public void   setRecordStatus(String v)         { this.recordStatus = v; }
}
