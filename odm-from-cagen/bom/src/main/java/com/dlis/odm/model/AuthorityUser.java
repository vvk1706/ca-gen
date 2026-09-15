package com.dlis.odm.model;

/**
 * BOM Class: AuthorityUser
 * Converted from CA Gen entity: AUTHORITY-USER.ENT
 *
 * Represents an authorised officer who can perform approvals in the DLIS workflow.
 * Evaluated during both approval ruleflows.
 */
public class AuthorityUser {

    /** System-generated primary key (AUTHORITY-USER-ID) */
    private long authorityUserId;

    /** Login / reference code — must be unique (USER-CODE) */
    private String userCode;

    /** Full name of the officer (USER-NAME) */
    private String userName;

    /** Organisation / department name (AUTHORITY-NAME) */
    private String authorityName;

    /**
     * Approval level (AUTHORITY-LEVEL).
     * Valid values: "1"=First Approver, "2"=Second Approver
     */
    private String authorityLevel;

    /** Specific department within the authority (DEPARTMENT) */
    private String department;

    /** Contact phone number (PHONE-NUMBER) */
    private String phoneNumber;

    /** Contact email address (EMAIL-ADDRESS) */
    private String emailAddress;

    /**
     * Active/Inactive flag (ACTIVE-STATUS).
     * Valid values: "A"=Active, "I"=Inactive
     */
    private String activeStatus;

    /**
     * License types this officer may approve (LICENSE-TYPES-AUTHORISED).
     * Examples: "L"=Learner only, "LP"=Learner+Probation, "LPO"=All types
     */
    private String licenseTypesAuthorised;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getAuthorityUserId() { return authorityUserId; }
    public void setAuthorityUserId(long authorityUserId) { this.authorityUserId = authorityUserId; }

    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }

    public String getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(String authorityLevel) { this.authorityLevel = authorityLevel; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }

    public String getLicenseTypesAuthorised() { return licenseTypesAuthorised; }
    public void setLicenseTypesAuthorised(String licenseTypesAuthorised) {
        this.licenseTypesAuthorised = licenseTypesAuthorised;
    }
}
