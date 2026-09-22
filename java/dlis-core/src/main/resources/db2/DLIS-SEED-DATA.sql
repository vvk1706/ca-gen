-- ============================================================
-- DLIS DB2 DDL — Reference Data (seed)
-- ============================================================

-- Fee schedule seed data
INSERT INTO DLIS.LICENSE_FEE_SCHEDULE
    (LICENSE_TYPE, FEE_TYPE, FEE_AMOUNT, EFFECTIVE_DATE, CURRENCY_CODE, DESCRIPTION, ACTIVE_STATUS)
VALUES
    ('L', 'IF', 45.00, '2024-01-01', 'USD', 'Learner License Issue Fee',     'A'),
    ('P', 'IF', 75.00, '2024-01-01', 'USD', 'Probation License Issue Fee',   'A'),
    ('O', 'IF', 95.00, '2024-01-01', 'USD', 'Open License Issue Fee',        'A'),
    ('L', 'RF', 40.00, '2024-01-01', 'USD', 'Learner License Renewal Fee',   'A'),
    ('P', 'RF', 65.00, '2024-01-01', 'USD', 'Probation License Renewal Fee', 'A'),
    ('O', 'RF', 85.00, '2024-01-01', 'USD', 'Open License Renewal Fee',      'A'),
    ('L', 'LF', 20.00, '2024-01-01', 'USD', 'Late Processing Fee - Learner', 'A'),
    ('P', 'LF', 25.00, '2024-01-01', 'USD', 'Late Processing Fee - Probation','A'),
    ('O', 'LF', 30.00, '2024-01-01', 'USD', 'Late Processing Fee - Open',    'A');

COMMIT;
