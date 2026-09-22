-- ============================================================
-- DLIS DB2 DDL for Java / zLinux deployment
-- Schema: DLIS
-- Compatible with: DB2 for z/OS v12+ and DB2 LUW 11.5 (zLinux)
-- ============================================================

CREATE SCHEMA DLIS;

-- ============================================================
-- Table: CANDIDATE
-- ============================================================
CREATE TABLE DLIS.CANDIDATE (
    CANDIDATE_ID        DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    FIRST_NAME          VARCHAR(30)     NOT NULL,
    LAST_NAME           VARCHAR(30)     NOT NULL,
    DATE_OF_BIRTH       DATE            NOT NULL,
    ID_NUMBER           VARCHAR(20)     NOT NULL,
    ADDRESS_LINE_1      VARCHAR(50)     NOT NULL,
    ADDRESS_LINE_2      VARCHAR(50),
    CITY                VARCHAR(30)     NOT NULL,
    STATE_PROVINCE      VARCHAR(30)     NOT NULL,
    POSTAL_CODE         VARCHAR(10)     NOT NULL,
    COUNTRY             VARCHAR(30)     NOT NULL,
    PHONE_NUMBER        VARCHAR(15),
    EMAIL_ADDRESS       VARCHAR(60),
    CREATED_DATE        DATE            NOT NULL,
    RECORD_STATUS       CHAR(1)         NOT NULL DEFAULT 'A',
    CONSTRAINT PK_CANDIDATE PRIMARY KEY (CANDIDATE_ID),
    CONSTRAINT CHK_CAND_STATUS  CHECK (RECORD_STATUS IN ('A','I')),
    CONSTRAINT CHK_CAND_DOB     CHECK (DATE_OF_BIRTH < CURRENT DATE)
);

-- ============================================================
-- Table: LICENSE_APPLICATION
-- ============================================================
CREATE TABLE DLIS.LICENSE_APPLICATION (
    APPLICATION_ID          DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                            (START WITH 1000, INCREMENT BY 1),
    CANDIDATE_ID            DECIMAL(10,0)   NOT NULL,
    LICENSE_TYPE            CHAR(1)         NOT NULL,
    APPLICATION_DATE        DATE            NOT NULL,
    APPLICATION_STATUS      CHAR(2)         NOT NULL DEFAULT 'PE',
    ELIGIBILITY_CHK_STATUS  CHAR(1)                  DEFAULT 'U',
    ELIGIBILITY_CHK_DATE    DATE,
    ELIGIBILITY_CHK_NOTES   VARCHAR(200),
    HISTORY_CHK_STATUS      CHAR(1)                  DEFAULT 'U',
    HISTORY_CHK_DATE        DATE,
    HISTORY_CHK_NOTES       VARCHAR(200),
    PAYMENT_STATUS          CHAR(1)                  DEFAULT 'U',
    PAYMENT_REFERENCE       VARCHAR(20),
    APPROVAL_1_STATUS       CHAR(1)                  DEFAULT 'U',
    APPROVAL_1_AUTHORITY    VARCHAR(50),
    APPROVAL_1_DATE         DATE,
    APPROVAL_1_NOTES        VARCHAR(200),
    APPROVAL_2_STATUS       CHAR(1)                  DEFAULT 'U',
    APPROVAL_2_AUTHORITY    VARCHAR(50),
    APPROVAL_2_DATE         DATE,
    APPROVAL_2_NOTES        VARCHAR(200),
    REJECTION_REASON        VARCHAR(300),
    CREATED_DATE            DATE            NOT NULL,
    LAST_UPDATED_DATE       DATE            NOT NULL,
    CREATED_BY              VARCHAR(20)     NOT NULL,
    LAST_UPDATED_BY         VARCHAR(20)     NOT NULL,
    CONSTRAINT PK_LICAPP    PRIMARY KEY (APPLICATION_ID),
    CONSTRAINT FK_LICAPP_CAND FOREIGN KEY (CANDIDATE_ID)
                              REFERENCES DLIS.CANDIDATE(CANDIDATE_ID),
    CONSTRAINT CHK_LICAPP_TYPE   CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_LICAPP_STATUS CHECK (APPLICATION_STATUS IN
                                        ('PE','EC','HC','PP','PA','A1','A2','AP','RE','IS')),
    CONSTRAINT CHK_ELIG_STATUS   CHECK (ELIGIBILITY_CHK_STATUS IN ('P','F','U')),
    CONSTRAINT CHK_HIST_STATUS   CHECK (HISTORY_CHK_STATUS IN ('P','F','U')),
    CONSTRAINT CHK_PAY_STATUS    CHECK (PAYMENT_STATUS IN ('P','U','W')),
    CONSTRAINT CHK_APP1_STATUS   CHECK (APPROVAL_1_STATUS IN ('A','R','U')),
    CONSTRAINT CHK_APP2_STATUS   CHECK (APPROVAL_2_STATUS IN ('A','R','U'))
);

-- ============================================================
-- Table: DRIVING_HISTORY
-- ============================================================
CREATE TABLE DLIS.DRIVING_HISTORY (
    HISTORY_ID              DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                            (START WITH 1, INCREMENT BY 1),
    CANDIDATE_ID            DECIMAL(10,0)   NOT NULL,
    INCIDENT_DATE           DATE            NOT NULL,
    INCIDENT_TYPE           CHAR(2)         NOT NULL,
    INCIDENT_DESCRIPTION    VARCHAR(300),
    DEMERIT_POINTS          DECIMAL(3,0)             DEFAULT 0,
    FINE_AMOUNT             DECIMAL(10,2)            DEFAULT 0,
    FINE_PAID_STATUS        CHAR(1),
    SUSPENSION_START_DATE   DATE,
    SUSPENSION_END_DATE     DATE,
    COURT_CASE_NUMBER       VARCHAR(20),
    RECORDED_BY_AUTHORITY   VARCHAR(50),
    RECORD_STATUS           CHAR(1)         NOT NULL DEFAULT 'A',
    CONSTRAINT PK_DHIST        PRIMARY KEY (HISTORY_ID),
    CONSTRAINT FK_DHIST_CAND   FOREIGN KEY (CANDIDATE_ID)
                               REFERENCES DLIS.CANDIDATE(CANDIDATE_ID),
    CONSTRAINT CHK_DHIST_TYPE  CHECK (INCIDENT_TYPE IN ('OF','AC','SU','DQ')),
    CONSTRAINT CHK_DHIST_PAID  CHECK (FINE_PAID_STATUS IN ('Y','N')),
    CONSTRAINT CHK_DHIST_STAT  CHECK (RECORD_STATUS IN ('A','I')),
    CONSTRAINT CHK_DHIST_DEM   CHECK (DEMERIT_POINTS >= 0),
    CONSTRAINT CHK_DHIST_FINE  CHECK (FINE_AMOUNT >= 0)
);

-- ============================================================
-- Table: ISSUED_LICENSE
-- ============================================================
CREATE TABLE DLIS.ISSUED_LICENSE (
    LICENSE_ID              DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                            (START WITH 1, INCREMENT BY 1),
    APPLICATION_ID          DECIMAL(10,0)   NOT NULL,
    CANDIDATE_ID            DECIMAL(10,0)   NOT NULL,
    LICENSE_NUMBER          VARCHAR(20)     NOT NULL,
    LICENSE_TYPE            CHAR(1)         NOT NULL,
    ISSUE_DATE              DATE            NOT NULL,
    EXPIRY_DATE             DATE            NOT NULL,
    LICENSE_STATUS          CHAR(1)         NOT NULL DEFAULT 'A',
    VEHICLE_CLASS           CHAR(2)         NOT NULL,
    RESTRICTIONS            VARCHAR(200),
    DEMERIT_BALANCE         DECIMAL(3,0)             DEFAULT 12,
    ISSUED_BY_AUTHORITY     VARCHAR(50)     NOT NULL,
    ISSUED_BY_OFFICER       VARCHAR(50)     NOT NULL,
    RENEWAL_COUNT           DECIMAL(3,0)             DEFAULT 0,
    PREVIOUS_LICENSE_ID     DECIMAL(10,0),
    NOTES                   VARCHAR(300),
    CONSTRAINT PK_ISSLIC       PRIMARY KEY (LICENSE_ID),
    CONSTRAINT FK_ISSLOC_APP   FOREIGN KEY (APPLICATION_ID)
                               REFERENCES DLIS.LICENSE_APPLICATION(APPLICATION_ID),
    CONSTRAINT FK_ISSLOC_CAND  FOREIGN KEY (CANDIDATE_ID)
                               REFERENCES DLIS.CANDIDATE(CANDIDATE_ID),
    CONSTRAINT UQ_LIC_NUMBER   UNIQUE (LICENSE_NUMBER),
    CONSTRAINT CHK_LIC_TYPE    CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_LIC_STATUS  CHECK (LICENSE_STATUS IN ('A','S','E','C','R')),
    CONSTRAINT CHK_VEH_CLASS   CHECK (VEHICLE_CLASS IN ('A','B','C','D')),
    CONSTRAINT CHK_LIC_EXPIRY  CHECK (EXPIRY_DATE > ISSUE_DATE),
    CONSTRAINT CHK_LIC_DEM     CHECK (DEMERIT_BALANCE >= 0)
);

-- ============================================================
-- Table: PAYMENT
-- ============================================================
CREATE TABLE DLIS.PAYMENT (
    PAYMENT_ID          DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    APPLICATION_ID      DECIMAL(10,0)   NOT NULL,
    CANDIDATE_ID        DECIMAL(10,0)   NOT NULL,
    PAYMENT_DATE        DATE            NOT NULL,
    PAYMENT_AMOUNT      DECIMAL(10,2)   NOT NULL,
    PAYMENT_METHOD      CHAR(2)         NOT NULL,
    PAYMENT_REFERENCE   VARCHAR(30)     NOT NULL,
    PAYMENT_STATUS      CHAR(1)         NOT NULL DEFAULT 'P',
    LICENSE_TYPE        CHAR(1)         NOT NULL,
    FEE_TYPE            CHAR(2)         NOT NULL,
    RECEIPT_NUMBER      VARCHAR(20),
    PROCESSED_BY        VARCHAR(20),
    NOTES               VARCHAR(200),
    CONSTRAINT PK_PAYMENT        PRIMARY KEY (PAYMENT_ID),
    CONSTRAINT FK_PAY_APP        FOREIGN KEY (APPLICATION_ID)
                                 REFERENCES DLIS.LICENSE_APPLICATION(APPLICATION_ID),
    CONSTRAINT FK_PAY_CAND       FOREIGN KEY (CANDIDATE_ID)
                                 REFERENCES DLIS.CANDIDATE(CANDIDATE_ID),
    CONSTRAINT CHK_PAY_METHOD    CHECK (PAYMENT_METHOD IN ('CC','DC','EF','CS','CH')),
    CONSTRAINT CHK_PAY_STATUS    CHECK (PAYMENT_STATUS IN ('S','F','R','P')),
    CONSTRAINT CHK_PAY_LICTYPE   CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_PAY_FEETYPE   CHECK (FEE_TYPE IN ('IF','RF','LF','PF')),
    CONSTRAINT CHK_PAY_AMOUNT    CHECK (PAYMENT_AMOUNT > 0)
);

-- ============================================================
-- Table: LICENSE_FEE_SCHEDULE
-- ============================================================
CREATE TABLE DLIS.LICENSE_FEE_SCHEDULE (
    FEE_SCHEDULE_ID     DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    LICENSE_TYPE        CHAR(1)         NOT NULL,
    FEE_TYPE            CHAR(2)         NOT NULL,
    FEE_AMOUNT          DECIMAL(10,2)   NOT NULL,
    EFFECTIVE_DATE      DATE            NOT NULL,
    EXPIRY_DATE         DATE,
    CURRENCY_CODE       CHAR(3)         NOT NULL DEFAULT 'USD',
    DESCRIPTION         VARCHAR(200),
    ACTIVE_STATUS       CHAR(1)         NOT NULL DEFAULT 'A',
    CONSTRAINT PK_FEESCHED       PRIMARY KEY (FEE_SCHEDULE_ID),
    CONSTRAINT CHK_FEE_LICTYPE   CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_FEE_FEETYPE   CHECK (FEE_TYPE IN ('IF','RF','LF','PF')),
    CONSTRAINT CHK_FEE_AMOUNT    CHECK (FEE_AMOUNT > 0),
    CONSTRAINT CHK_FEE_ACTIVE    CHECK (ACTIVE_STATUS IN ('A','I'))
);

-- ============================================================
-- Table: AUTHORITY_USER
-- ============================================================
CREATE TABLE DLIS.AUTHORITY_USER (
    AUTHORITY_USER_ID       DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                            (START WITH 1, INCREMENT BY 1),
    USER_CODE               VARCHAR(20)     NOT NULL,
    USER_NAME               VARCHAR(60)     NOT NULL,
    AUTHORITY_NAME          VARCHAR(50)     NOT NULL,
    AUTHORITY_LEVEL         CHAR(1)         NOT NULL,
    DEPARTMENT              VARCHAR(50),
    PHONE_NUMBER            VARCHAR(15),
    EMAIL_ADDRESS           VARCHAR(60),
    ACTIVE_STATUS           CHAR(1)         NOT NULL DEFAULT 'A',
    LICENSE_TYPES_AUTHORISED VARCHAR(3),
    CONSTRAINT PK_AUTHUSER       PRIMARY KEY (AUTHORITY_USER_ID),
    CONSTRAINT UQ_AUTHUSER_CODE  UNIQUE (USER_CODE),
    CONSTRAINT CHK_AUTH_LEVEL    CHECK (AUTHORITY_LEVEL IN ('1','2')),
    CONSTRAINT CHK_AUTH_ACTIVE   CHECK (ACTIVE_STATUS IN ('A','I'))
);
