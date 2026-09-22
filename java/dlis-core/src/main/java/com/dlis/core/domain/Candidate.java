package com.dlis.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for the CANDIDATE entity.
 * Maps to DLIS.CANDIDATE in DB2.
 * Derived from CA Gen encyclopedia entity: CANDIDATE.ENT
 */
public class Candidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long   candidateId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String idNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String emailAddress;
    private LocalDate createdDate;
    private String recordStatus;   // A=Active, I=Inactive

    public Candidate() {}

    // ---- Getters / Setters ----

    public Long getCandidateId()                     { return candidateId; }
    public void setCandidateId(Long candidateId)     { this.candidateId = candidateId; }

    public String getFirstName()                     { return firstName; }
    public void setFirstName(String firstName)       { this.firstName = firstName; }

    public String getLastName()                      { return lastName; }
    public void setLastName(String lastName)         { this.lastName = lastName; }

    public LocalDate getDateOfBirth()                { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth){ this.dateOfBirth = dateOfBirth; }

    public String getIdNumber()                      { return idNumber; }
    public void setIdNumber(String idNumber)         { this.idNumber = idNumber; }

    public String getAddressLine1()                  { return addressLine1; }
    public void setAddressLine1(String v)            { this.addressLine1 = v; }

    public String getAddressLine2()                  { return addressLine2; }
    public void setAddressLine2(String v)            { this.addressLine2 = v; }

    public String getCity()                          { return city; }
    public void setCity(String city)                 { this.city = city; }

    public String getStateProvince()                 { return stateProvince; }
    public void setStateProvince(String v)           { this.stateProvince = v; }

    public String getPostalCode()                    { return postalCode; }
    public void setPostalCode(String v)              { this.postalCode = v; }

    public String getCountry()                       { return country; }
    public void setCountry(String v)                 { this.country = v; }

    public String getPhoneNumber()                   { return phoneNumber; }
    public void setPhoneNumber(String v)             { this.phoneNumber = v; }

    public String getEmailAddress()                  { return emailAddress; }
    public void setEmailAddress(String v)            { this.emailAddress = v; }

    public LocalDate getCreatedDate()                { return createdDate; }
    public void setCreatedDate(LocalDate v)          { this.createdDate = v; }

    public String getRecordStatus()                  { return recordStatus; }
    public void setRecordStatus(String v)            { this.recordStatus = v; }

    public boolean isActive() {
        return "A".equals(recordStatus);
    }
}
