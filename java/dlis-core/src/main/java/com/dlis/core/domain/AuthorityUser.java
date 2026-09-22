package com.dlis.core.domain;

import java.io.Serializable;

/**
 * Domain model for AUTHORITY_USER entity.
 * Maps to DLIS.AUTHORITY_USER in DB2.
 */
public class AuthorityUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long    authorityUserId;
    private String  userCode;
    private String  userName;
    private String  authorityName;
    private String  authorityLevel;         // 1=First Approver, 2=Second Approver
    private String  department;
    private String  phoneNumber;
    private String  emailAddress;
    private String  activeStatus;           // A, I
    private String  licenseTypesAuthorised; // e.g. "LPO", "L", "PO"

    public AuthorityUser() {}

    public boolean isActive() {
        return "A".equals(activeStatus);
    }

    public boolean isAuthorisedFor(String licenseType) {
        return licenseTypesAuthorised != null
            && licenseTypesAuthorised.contains(licenseType);
    }

    public boolean isLevel1() { return "1".equals(authorityLevel); }
    public boolean isLevel2() { return "2".equals(authorityLevel); }

    // ---- Getters / Setters ----

    public Long getAuthorityUserId()                        { return authorityUserId; }
    public void setAuthorityUserId(Long v)                  { this.authorityUserId = v; }

    public String getUserCode()                             { return userCode; }
    public void setUserCode(String v)                       { this.userCode = v; }

    public String getUserName()                             { return userName; }
    public void setUserName(String v)                       { this.userName = v; }

    public String getAuthorityName()                        { return authorityName; }
    public void setAuthorityName(String v)                  { this.authorityName = v; }

    public String getAuthorityLevel()                       { return authorityLevel; }
    public void setAuthorityLevel(String v)                 { this.authorityLevel = v; }

    public String getDepartment()                           { return department; }
    public void setDepartment(String v)                     { this.department = v; }

    public String getPhoneNumber()                          { return phoneNumber; }
    public void setPhoneNumber(String v)                    { this.phoneNumber = v; }

    public String getEmailAddress()                         { return emailAddress; }
    public void setEmailAddress(String v)                   { this.emailAddress = v; }

    public String getActiveStatus()                         { return activeStatus; }
    public void setActiveStatus(String v)                   { this.activeStatus = v; }

    public String getLicenseTypesAuthorised()               { return licenseTypesAuthorised; }
    public void setLicenseTypesAuthorised(String v)         { this.licenseTypesAuthorised = v; }
}
