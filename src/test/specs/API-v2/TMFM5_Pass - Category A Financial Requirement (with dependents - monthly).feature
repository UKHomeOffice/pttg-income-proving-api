Feature: Pass - Category A Financial Requirement - with dependants - monthly

    Requirement to meet Category A
    Applicant or Sponsor has received 6 consecutive monthly payments from the same employer over the 6 month period prior to the Application Raised Date

    Financial income regulation to pass this Feature File
    Income required amount no dependant child = £18600 (They have earned 6 monthly payments => £1550 Monthly Gross Income in the 6 months prior to the Application Raised Date)
    Additional funds for 1 dependant child = £3800 on top of employment threshold
    Additional funds for EVERY subsequent dependant child = £2400 on top of employment threshold per child

    Financial income calculation to pass this Feature File
    Income required amount + 1 dependant amount + (Additional dependant amount * number of dependants)/12 = Equal to or greater than the threshold Monthly Gross Income in the 6 months prior to the Application Raised Date

    1 Dependant child - £18600+£3800/12 = £1866.67
    2 Dependant children - £18600+£3800+£2400/12 = £2066.67
    3 Dependant children - £18600+£3800+(£2400*2)/12 = £2266.67
    5 Dependant children - £18600+£3800+(£2400*4)/12 = £2666.67
    7 Dependant children - £18600+£3800+(£2400*6)/12 = £3066.67
    ETC

    Background: Thresholds are configured to default values
        Given The yearly threshold is configured to 18600:
        And The single dependant yearly threshold is configured to 22400:
        And The remaining dependants increment is configured to 2400:

#New scenario -
    Scenario: Tony Ledo meets the Category A Financial Requirement with 1 dependant

    Pay date 15th of the month
    Before day of application received date
    He earns £4166.67 Monthly Gross Income EVERY of the 6 months
    He has 1 dependant child

        Given HMRC has the following income records:
            | Date       | Amount  | Week Number | Month Number | PAYE Reference | Employer         |
            | 2015-01-15 | 4166.67 |             | 06           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-12-15 | 4166.67 |             | 05           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-11-15 | 4166.67 |             | 04           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-10-15 | 4166.67 |             | 03           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-09-15 | 4166.67 |             | 02           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-08-15 | 4166.67 |             | 01           | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v3 TM Family API is invoked with the following:
            | NINO - Applicant        | TL123456A  |
            | Application raised date | 2015-01-23 |
            | Dependants              | 1          |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Response             | HTTP Status               | 200              |
            | Applicant                 | National Insurance Number | TL123456A        |
            | Category A Monthly Salary | Financial requirement met | true             |
            | Category A Monthly Salary | Assessment start date     | 2014-07-23       |
            | Category A Monthly Salary | Application Raised date   | 2015-01-23       |
            | Category A Monthly Salary | Threshold                 | 1866.67          |
            | Category A Monthly Salary | Employer Name - TL123456A | Flying Pizza Ltd |

#New scenario -
    Scenario: Scarlett Jones meets the Category A Financial Requirement with 3 dependant

    Pay date 2nd of the month
    Before day of Application Raised Date
    He earns £3333.33 Monthly Gross Income EVERY of the 6 months
    He has 3 dependant child

        Given HMRC has the following income records:
            | Date       | Amount  | Week Number | Month Number | PAYE Reference | Employer         |
            | 2015-12-02 | 3333.33 |             | 06           | FP/Ref1        | Flying Pizza Ltd |
            | 2015-11-02 | 3333.33 |             | 05           | FP/Ref1        | Flying Pizza Ltd |
            | 2015-10-02 | 3333.33 |             | 04           | FP/Ref1        | Flying Pizza Ltd |
            | 2015-09-02 | 3333.33 |             | 03           | FP/Ref1        | Flying Pizza Ltd |
            | 2015-08-02 | 3333.33 |             | 02           | FP/Ref1        | Flying Pizza Ltd |
            | 2015-07-02 | 3333.33 |             | 01           | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v3 TM Family API is invoked with the following:
            | NINO - Applicant        | SJ123456C  |
            | Application raised date | 2015-12-08 |
            | Dependants              | 3          |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Response             | HTTP Status               | 200              |
            | Applicant                 | National Insurance Number | SJ123456C        |
            | Category A Monthly Salary | Financial requirement met | true             |
            | Category A Monthly Salary | Assessment start date     | 2015-06-08       |
            | Category A Monthly Salary | Application Raised date   | 2015-12-08       |
            | Category A Monthly Salary | Threshold                 | 2266.67          |
            | Category A Monthly Salary | Employer Name - SJ123456C | Flying Pizza Ltd |
#New scenario -
    Scenario: Wasim Mohammed meets the Category A Financial Requirement with 5 dependants

    Pay date 30th of the month
    On the same day of Application Raised Date
    He earns £5833.33 Monthly Gross Income EVERY of the 6 months
    He has 5 dependant child

        Given HMRC has the following income records:
            | Date       | Amount  | Week Number | Month Number | PAYE Reference | Employer         |
            | 2015-01-30 | 5833.33 |             | 06           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-12-30 | 5833.33 |             | 05           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-11-30 | 5833.33 |             | 04           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-10-30 | 5833.33 |             | 03           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-09-30 | 5833.33 |             | 02           | FP/Ref1        | Flying Pizza Ltd |
            | 2014-08-30 | 5833.33 |             | 01           | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v3 TM Family API is invoked with the following:
            | NINO - Applicant        | WA987654B  |
            | Application raised date | 2015-02-28 |
            | Dependants              | 5          |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Response             | HTTP Status               | 200              |
            | Applicant                 | National Insurance Number | WA987654B        |
            | Category A Monthly Salary | Financial requirement met | true             |
            | Category A Monthly Salary | Assessment start date     | 2014-08-28       |
            | Category A Monthly Salary | Application Raised date   | 2015-02-28       |
            | Category A Monthly Salary | Threshold                 | 2666.67          |
            | Category A Monthly Salary | Employer Name - WA987654B | Flying Pizza Ltd |
