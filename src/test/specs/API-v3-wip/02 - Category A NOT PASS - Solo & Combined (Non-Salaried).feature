Feature: Category A Financial Requirement - Solo & Combined Applications

    # JIRA STORY: EE-3838
    # Scenarios where Category A Non-Salaried Assessments do not pass
    # Annual threshold is £18,600 if no dependents are included. Addition of a first dependent child adds £3,800 then each thereafter a further £2,400.
    # Formula for calculating the income for comparison against the threshold is: Total all payments, divide by 6, multiply by 12

    # BACKGROUND: Applications with one applicant will be required to meet a main threshold value of £18,600.
    #             Applications with one dependant will be required to meet an amended threshold value of £22,400
    #             Applications with two dependants will be required to meet a further amended threshold value of £24,800

    Scenario: 01 Cheryl has no dependents. Her income history shows payments in 6 months that do not meet the threshold. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-27 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-03-29 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-02-28 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-01-31 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-12-29 | 1299.50 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-11-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO                    | VY348368A  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200              |
            | Financial requirement met | false            |
            | Failure Reason            | Below Threshold  |
            | Application Raised date   | 2018-04-30       |
            | National Insurance Number | VY348368A        |
            | Threshold                 | 18600            |
            | Employer Name             | Flying Pizza Ltd |

    ############

    Scenario: 02 Ashley has one dependent. His income history shows payments in 7 months that do not meet the threshold. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-30 |  500.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-03-29 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-02-28 | 4000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-01-31 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-12-29 | 1199.50 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-11-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-10-30 |  500.00 | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO                    | VB345624B  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200              |
            | Financial requirement met | false            |
            | Failure Reason            | Below Threshold  |
            | Application Raised date   | 2018-04-30       |
            | National Insurance Number | VB345624B        |
            | Threshold                 | 22400            |
            | Employer Name             | Flying Pizza Ltd |

       ############

    Scenario: 03 Carla has no dependents. Her income history shows payments in 7 months that meet the threshold however one payment is just outside the assessment range. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-30 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-03-29 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-02-28 | 2000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-01-31 | 1300.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-12-29 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-11-30 |  999.99 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-10-29 |  000.01 | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO                    | FD345678A  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200              |
            | Category                  | A                |
            | Financial requirement met | false            |
            | Failure Reason            | Below Threshold  |
            | Application Raised date   | 2018-04-30       |
            | National Insurance Number | FD345678A        |
            | Threshold                 | 18600            |
            | Employer Name             | Flying Pizza Ltd |

    ############

    Scenario: 04 Kayleigh has no dependants. Her income history shows payments that only meet the threshold with combined income from multiple employers. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer          |
            | 2018-03-29 | 2000.00 | FP/Ref1        | Flying Pizza Ltd  |
            | 2018-02-28 | 2000.00 | FP/Ref1        | Flying Pizza Ltd  |
            | 2018-01-31 | 2000.00 | FP/Ref1        | Flying Pizza Ltd  |
            | 2017-12-29 | 2000.00 | FP/Ref2        | Derek's Autos Ltd |
            | 2017-11-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd  |
            | 2017-10-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd  |


        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO                    | KS379678A  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200                                 |
            | Financial requirement met | false                               |
            | Failure Reason            | Multiple Employers                  |
            | Application Raised date   | 2018-04-30                          |
            | National Insurance Number | KS379678A                           |
            | Threshold                 | 18600                               |
            | Employer Name             | Flying Pizza Ltd, Derek's Autos Ltd |

    ############

    Scenario: 05 Barry has no dependents. His income history shows a payment in some months with gaps but do not meet the threshold even when it is supplemented by a partners income also having gaps. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-27 | 2450.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-02-28 |  550.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2018-01-31 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-11-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |

        And the applicants partner has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-27 | 1000.00 | HO/Ref9        | The Home Office  |
            | 2018-02-28 | 1000.00 | HO/Ref9        | The Home Office  |
            | 2018-01-31 | 2000.00 | HO/Ref9        | The Home Office  |
            | 2017-12-23 |  299.99 | HO/Ref9        | The Home Office  |

        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO - Applicant        | AC345118A  |
            | NINO - Partner          | SC382638G  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200              |
            | Financial requirement met | false            |
            | Failure Reason            | Below Threshold  |
            | Application Raised date   | 2018-04-30       |
            | National Insurance Number | AC345118A        |
            | National Insurance Number | SC382638G        |
            | Threshold                 | 18600            |
            | Employer Name             | Flying Pizza Ltd |

    ############

    Scenario: 06 Sherilyn has two dependents. Her income history shows a full contingent of payments over a 12 month period. All 12 months average as a pass against the threshold but the payments within the 6 month period do not. Assessment range 2018-04-30 to 2017-10-30

        Given HMRC has the following income records:
            | Date       | Amount  | PAYE Reference | Employer         |
            | 2018-04-27 | 400.00  | FP/Ref1        | Flying Pizza Ltd |
            | 2018-03-29 | 300.00  | FP/Ref1        | Flying Pizza Ltd |
            | 2018-02-23 | 300.00  | FP/Ref1        | Flying Pizza Ltd |
            | 2018-01-26 | 300.00  | FP/Ref1        | Flying Pizza Ltd |
            | 2017-12-27 |  50.00  | FP/Ref1        | Flying Pizza Ltd |
            | 2017-11-28 |  49.99  | FP/Ref1        | Flying Pizza Ltd |
            | 2017-10-31 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-09-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-08-27 | 2500.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-07-28 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-06-31 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |
            | 2017-05-30 | 1000.00 | FP/Ref1        | Flying Pizza Ltd |

        When the Income Proving v2 TM Family API is invoked with the following:
            | NINO - Applicant        | HG345118A  |
            | Application Raised Date | 2018-04-30 |

        Then The Income Proving TM Family API provides the following result:
            | HTTP Status               | 200              |
            | Financial requirement met | false            |
            | Failure Reason            | Below Threshold  |
            | Application Raised date   | 2018-04-30       |
            | National Insurance Number | HG345118A        |
            | Threshold                 | 24800            |
            | Employer Name             | Flying Pizza Ltd |
