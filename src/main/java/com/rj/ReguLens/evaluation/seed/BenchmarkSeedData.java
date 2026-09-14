package com.rj.ReguLens.evaluation.seed;

import com.rj.ReguLens.entity.EvaluationBenchmarkSet;

import java.util.List;

public class BenchmarkSeedData {

    public static List<EvaluationBenchmarkSet> getGoldenBenchmarkCases() {
        return List.of(
                // 1. GDPR Article 17 - Erasure
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the conditions under which a data subject can request erasure of their personal data?")
                        .expectedClauseReferences(List.of("Article 17(1)", "Article 17"))
                        .groundTruthAnswer("Under GDPR Article 17, data subjects have the right to obtain erasure of personal data without undue delay when data is no longer necessary, consent is withdrawn, or data was unlawfully processed.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 2. GDPR Article 33 - Breach Notification
                EvaluationBenchmarkSet.builder()
                        .queryText("What is the statutory deadline for notifying the supervisory authority of a personal data breach?")
                        .expectedClauseReferences(List.of("Article 33(1)", "Article 33"))
                        .groundTruthAnswer("In the case of a personal data breach, the controller shall without undue delay and, where feasible, not later than 72 hours after having become aware of it, notify the supervisory authority.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 3. GDPR Article 32 - Security of Processing
                EvaluationBenchmarkSet.builder()
                        .queryText("What technical and organisational security measures are mandated for processing personal data?")
                        .expectedClauseReferences(List.of("Article 32(1)", "Article 32"))
                        .groundTruthAnswer("GDPR Article 32 mandates pseudonimisation, encryption of personal data, continuous confidentiality, integrity, availability, resilience of processing systems, and regular security testing.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 4. GDPR Article 25 - Data Protection by Design
                EvaluationBenchmarkSet.builder()
                        .queryText("How must organizations implement data protection by design and by default?")
                        .expectedClauseReferences(List.of("Article 25(1)", "Article 25(2)", "Article 25"))
                        .groundTruthAnswer("Organizations must implement appropriate technical and organizational measures, such as pseudonymisation, both at the time of the determination of the means for processing and at the time of the processing itself.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 5. GDPR Article 30 - Records of Processing
                EvaluationBenchmarkSet.builder()
                        .queryText("What records of processing activities (ROPA) must a controller maintain?")
                        .expectedClauseReferences(List.of("Article 30(1)", "Article 30"))
                        .groundTruthAnswer("Controllers must maintain written records containing the name and contact details of controller/DPO, purposes of processing, categories of data subjects and data, recipients, and security measures.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 6. GDPR Article 35 - DPIA
                EvaluationBenchmarkSet.builder()
                        .queryText("When is a Data Protection Impact Assessment (DPIA) mandatory?")
                        .expectedClauseReferences(List.of("Article 35(1)", "Article 35(3)", "Article 35"))
                        .groundTruthAnswer("A DPIA is required where a type of processing, in particular using new technologies, is likely to result in a high risk to the rights and freedoms of natural persons, including systematic automated profiling.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 7. GDPR Article 15 - Right of Access
                EvaluationBenchmarkSet.builder()
                        .queryText("What information must be provided when a data subject exercises their right of access?")
                        .expectedClauseReferences(List.of("Article 15(1)", "Article 15"))
                        .groundTruthAnswer("The controller must confirm whether personal data is processed, and provide access to the data, purposes of processing, categories of data, recipients, and the envisaged retention period.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 8. GDPR Article 6 - Lawfulness of Processing
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the six lawful bases for processing personal data under European regulations?")
                        .expectedClauseReferences(List.of("Article 6(1)", "Article 6"))
                        .groundTruthAnswer("Processing is lawful only if based on consent, contract performance, legal obligation, vital interests, public interest, or legitimate interests.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 9. GDPR Article 44 - International Transfers
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the general principles governing cross-border transfers of personal data to third countries?")
                        .expectedClauseReferences(List.of("Article 44", "Article 45", "Article 46"))
                        .groundTruthAnswer("Transfers of personal data to third countries may only take place if subject to an adequacy decision, appropriate safeguards (like Standard Contractual Clauses), or explicit derogations.")
                        .jurisdiction("EU")
                        .active(true)
                        .build(),

                // 10. HIPAA - Security Rule Technical Safeguards
                EvaluationBenchmarkSet.builder()
                        .queryText("What technical safeguards are required under HIPAA for electronic Protected Health Information (ePHI)?")
                        .expectedClauseReferences(List.of("Section 164.312", "HIPAA Security Rule"))
                        .groundTruthAnswer("Covered entities must implement access control, unique user identification, emergency access procedures, automatic logoff, encryption, integrity controls, and transmission security for ePHI.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 11. HIPAA - Breach Notification Rule
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the HIPAA notification requirements for a breach affecting 500 or more individuals?")
                        .expectedClauseReferences(List.of("Section 164.404", "Section 164.406", "Section 164.408"))
                        .groundTruthAnswer("Breaches affecting 500 or more individuals require notification to affected individuals and the HHS Secretary without unreasonable delay and in no case later than 60 calendar days, along with prominent media notice.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 12. HIPAA - Minimum Necessary Standard
                EvaluationBenchmarkSet.builder()
                        .queryText("How does the HIPAA Privacy Rule define the minimum necessary standard for PHI disclosures?")
                        .expectedClauseReferences(List.of("Section 164.502(b)", "Section 164.514(d)"))
                        .groundTruthAnswer("Covered entities must make reasonable efforts to limit the use, disclosure, and request of PHI to the minimum necessary to accomplish the intended purpose of the use or disclosure.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 13. HIPAA - Business Associate Agreement
                EvaluationBenchmarkSet.builder()
                        .queryText("When is a Business Associate Agreement (BAA) legally required under HIPAA?")
                        .expectedClauseReferences(List.of("Section 164.504(e)", "Section 164.502(e)"))
                        .groundTruthAnswer("A BAA is required before a covered entity allows a business associate to create, receive, maintain, or transmit PHI on the covered entity's behalf to ensure satisfactory assurances of safeguards.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 14. PCI-DSS v4.0 - Requirement 3 Data Protection
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the primary methods mandated by PCI-DSS Requirement 3 to protect stored cardholder data?")
                        .expectedClauseReferences(List.of("Requirement 3.4", "Requirement 3.5", "Requirement 3"))
                        .groundTruthAnswer("PCI-DSS Req 3 mandates rendering Primary Account Numbers (PAN) unreadable anywhere it is stored using strong cryptography, truncation, index tokens, or keyed cryptographic hashes.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 15. PCI-DSS v4.0 - Requirement 8 Multi-Factor Authentication
                EvaluationBenchmarkSet.builder()
                        .queryText("What multi-factor authentication (MFA) requirements are enforced by PCI-DSS Requirement 8?")
                        .expectedClauseReferences(List.of("Requirement 8.3", "Requirement 8.4", "Requirement 8"))
                        .groundTruthAnswer("MFA is required for all non-console administrative access into the cardholder data environment (CDE) and all remote access originating outside the entity's network.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 16. PCI-DSS v4.0 - Requirement 10 Audit Logging
                EvaluationBenchmarkSet.builder()
                        .queryText("What audit trail and log monitoring obligations are specified under PCI-DSS Requirement 10?")
                        .expectedClauseReferences(List.of("Requirement 10.2", "Requirement 10.3", "Requirement 10"))
                        .groundTruthAnswer("Audit logs must record all user access to cardholder data, root/admin actions, invalid access attempts, and audit trail modifications, retaining at least 12 months of logs with 3 months immediately available.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 17. SOC 2 - Common Criteria CC6.1 Access Controls
                EvaluationBenchmarkSet.builder()
                        .queryText("How does SOC 2 Common Criteria 6.1 enforce logical access security and account provisioning?")
                        .expectedClauseReferences(List.of("CC6.1", "CC6.2", "CC6.3"))
                        .groundTruthAnswer("CC6.1 requires organizations to restrict logical access to authorized personnel via role-based access controls, multi-factor authentication, and immediate revocation of access upon employee termination.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 18. SOC 2 - Common Criteria CC7.2 Incident & Vulnerability Management
                EvaluationBenchmarkSet.builder()
                        .queryText("What vulnerability scanning and intrusion monitoring controls are required under SOC 2 CC7.2?")
                        .expectedClauseReferences(List.of("CC7.2", "CC7.3"))
                        .groundTruthAnswer("Organizations must implement automated vulnerability scans, patch management schedules, intrusion detection systems (IDS), and documented incident response procedures.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 19. SOC 2 - Common Criteria CC8.1 Change Management
                EvaluationBenchmarkSet.builder()
                        .queryText("What change authorization and segregation of duties controls are mandated under SOC 2 CC8.1?")
                        .expectedClauseReferences(List.of("CC8.1"))
                        .groundTruthAnswer("CC8.1 requires that infrastructure and software changes follow formal approval workflows, automated peer code reviews, automated CI/CD testing, and separation of duties between developers and production deployment.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 20. SOX Section 404 - Internal Controls Reporting
                EvaluationBenchmarkSet.builder()
                        .queryText("What are management's statutory duties regarding internal controls evaluation under SOX Section 404?")
                        .expectedClauseReferences(List.of("Section 404(a)", "Section 404(b)", "Section 404"))
                        .groundTruthAnswer("Management must establish, maintain, and annually assess the effectiveness of internal control structures and financial reporting procedures, certified by registered public accounting firms.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 21. SOX Section 302 - Corporate Responsibility for Reports
                EvaluationBenchmarkSet.builder()
                        .queryText("What certifications must corporate executive officers make under SOX Section 302?")
                        .expectedClauseReferences(List.of("Section 302(a)", "Section 302"))
                        .groundTruthAnswer("The CEO and CFO must personally certify that they have reviewed the quarterly and annual financial reports, that reports contain no material misstatements, and that internal controls were evaluated within 90 days.")
                        .jurisdiction("US")
                        .active(true)
                        .build(),

                // 22. Policy Lifecycle - Expired Policy Query
                EvaluationBenchmarkSet.builder()
                        .queryText("What were the historical 2019 data retention periods under the legacy archiving policy?")
                        .expectedClauseReferences(List.of("Legacy Policy v1", "Section 9"))
                        .groundTruthAnswer("⚠️ Regulatory Notice: Policy 'Legacy Archiving Policy' (Version 1) expired. Historical records required 3-year tape backups.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 23. Missing Context / Threshold Rejection Control
                EvaluationBenchmarkSet.builder()
                        .queryText("What are the specific recipes and ingredient ratios for chocolate chip cookies in the employee cafeteria?")
                        .expectedClauseReferences(List.of())
                        .groundTruthAnswer("The indexed regulatory policy documents do not contain sufficient context to answer this compliance question.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build(),

                // 24. Missing Context / Space Exploration Out of Scope
                EvaluationBenchmarkSet.builder()
                        .queryText("How do orbital trajectory calculations apply to the James Webb Space Telescope deployment?")
                        .expectedClauseReferences(List.of())
                        .groundTruthAnswer("The indexed regulatory policy documents do not contain sufficient context to answer this compliance question.")
                        .jurisdiction("GLOBAL")
                        .active(true)
                        .build()
        );
    }
}
