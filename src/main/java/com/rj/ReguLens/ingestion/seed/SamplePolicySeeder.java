package com.rj.ReguLens.ingestion.seed;

import com.rj.ReguLens.entity.*;
import com.rj.ReguLens.ingestion.service.IngestionService;
import com.rj.ReguLens.repository.PolicyCategoryRepository;
import com.rj.ReguLens.repository.PolicyRepository;
import com.rj.ReguLens.repository.PolicyVersionRepository;
import com.rj.ReguLens.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SamplePolicySeeder {

    UserRepository userRepository;
    PolicyCategoryRepository categoryRepository;
    PolicyRepository policyRepository;
    PolicyVersionRepository versionRepository;
    IngestionService ingestionService;

    @Transactional
    public int seedSamplePolicies() {
        log.info("Starting sample regulatory policies ingestion...");

        // 1. Ensure default admin user exists
        User adminUser = userRepository.findAll().stream()
                .filter(u -> "compliance_admin".equals(u.getUsername()))
                .findFirst()
                .orElse(null);

        if (adminUser == null) {
            adminUser = userRepository.save(User.builder()
                    .username("compliance_admin")
                    .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
                    .name("Chief Compliance Officer")
                    .active(true)
                    .build());
        }
        final User admin = adminUser;

        // 2. Ensure Categories exist
        PolicyCategory catDataPrivacy = categoryRepository.findByName("Data Privacy");
        if (catDataPrivacy == null) {
            catDataPrivacy = categoryRepository.save(new PolicyCategory(null, "Data Privacy", new HashSet<>()));
        }

        PolicyCategory catSec = categoryRepository.findByName("Information Security");
        if (catSec == null) {
            catSec = categoryRepository.save(new PolicyCategory(null, "Information Security", new HashSet<>()));
        }

        // 3. GDPR Active Policy
        String gdprContent = """
                # General Data Protection Regulation (GDPR)
                
                ## Chapter III - Rights of the Data Subject
                
                ### Article 15 - Right of Access by the Data Subject
                1. The data subject shall have the right to obtain from the controller confirmation as to whether or not personal data concerning him or her are being processed, and, where that is the case, access to the personal data and the following information:
                (a) the purposes of the processing;
                (b) the categories of personal data concerned;
                (c) the recipients or categories of recipient to whom the personal data have been or will be disclosed.
                
                ### Article 17 - Right to Erasure ('Right to be Forgotten')
                1. The data subject shall have the right to obtain from the controller the erasure of personal data concerning him or her without undue delay and the controller shall have the obligation to erase personal data without undue delay where one of the following grounds applies:
                (a) the personal data are no longer necessary in relation to the purposes for which they were collected or otherwise processed;
                (b) the data subject withdraws consent on which the processing is based;
                (c) the data subject objects to the processing and there are no overriding legitimate grounds for the processing;
                (d) the personal data have been unlawfully processed.
                
                ## Chapter IV - Controller and Processor
                
                ### Article 32 - Security of Processing
                1. Taking into account the state of the art, the costs of implementation and the nature, scope, context and purposes of processing as well as the risk of varying likelihood and severity for the rights and freedoms of natural persons, the controller and the processor shall implement appropriate technical and organisational measures to ensure a level of security appropriate to the risk, including inter alia as appropriate:
                (a) the pseudonymisation and encryption of personal data;
                (b) the ability to ensure the ongoing confidentiality, integrity, availability and resilience of processing systems and services;
                (c) a process for regularly testing, assessing and evaluating the effectiveness of technical and organisational measures for ensuring the security of the processing.
                
                ### Article 33 - Notification of a Personal Data Breach to the Supervisory Authority
                1. In the case of a personal data breach, the controller shall without undue delay and, where feasible, not later than 72 hours after having become aware of it, notify the personal data breach to the supervisory authority competent in accordance with Article 55, unless the personal data breach is unlikely to result in a risk to the rights and freedoms of natural persons.
                2. Where the notification to the supervisory authority is not made within 72 hours, it shall be accompanied by reasons for the delay.
                """;

        Policy gdprPol = policyRepository.findAll().stream()
                .filter(p -> "General Data Protection Regulation".equals(p.getTitle()))
                .findFirst()
                .orElse(null);

        if (gdprPol == null) {
            gdprPol = policyRepository.save(Policy.builder()
                    .title("General Data Protection Regulation")
                    .description("European Union regulation on data protection and privacy.")
                    .owner(admin)
                    .active(true)
                    .categories(Set.of(catDataPrivacy))
                    .build());
        }
        final UUID gdprPolicyId = gdprPol.getId();

        PolicyVersion gdprVer = versionRepository.findAll().stream()
                .filter(v -> v.getPolicy().getId().equals(gdprPolicyId) && Integer.valueOf(1).equals(v.getVersion()))
                .findFirst()
                .orElse(null);

        if (gdprVer == null) {
            gdprVer = versionRepository.save(PolicyVersion.builder()
                    .policy(gdprPol)
                    .version(1)
                    .content(gdprContent)
                    .status(PolicyVersionStatusEnum.ACTIVE)
                    .approvedBy(admin)
                    .build());
        }

        List<DocumentChunk> gdprChunks = ingestionService.processAndPersistPolicyVersion(gdprVer.getId());

        // 4. HIPAA Active Policy
        String hipaaContent = """
                # Health Insurance Portability and Accountability Act (HIPAA)
                
                ## Security Rule - Technical Safeguards
                
                ### Section 164.312 - Technical Safeguards
                A covered entity or business associate must, in accordance with Section 164.306, implement technical policies and procedures for electronic information systems that maintain electronic protected health information to allow access only to those persons or software programs that have been granted access rights.
                1. Access Control. Implement technical policies and procedures for electronic information systems that maintain electronic protected health information to allow access only to those persons or software programs that have been granted access rights.
                (a) Unique user identification (Required). Assign a unique name and/or number for identifying and tracking user identity.
                (b) Emergency access procedure (Required). Establish procedures for obtaining necessary electronic protected health information during an emergency.
                (c) Automatic logoff (Addressable). Implement electronic procedures that terminate an electronic session after a predetermined time of inactivity.
                (d) Encryption and decryption (Addressable). Implement a mechanism to encrypt and decrypt electronic protected health information.
                
                ## Breach Notification Rule
                
                ### Section 164.404 - Notification to Individuals
                A covered entity shall, following the discovery of a breach of unsecured protected health information, notify each individual whose unsecured protected health information has been, or is reasonably believed by the covered entity to have been, accessed, acquired, used, or disclosed as a result of such breach.
                Timeliness: Notifications shall be provided without unreasonable delay and in no case later than 60 calendar days after discovery of a breach.
                """;

        Policy hipaaPol = policyRepository.findAll().stream()
                .filter(p -> "HIPAA Security & Privacy Standards".equals(p.getTitle()))
                .findFirst()
                .orElse(null);

        if (hipaaPol == null) {
            hipaaPol = policyRepository.save(Policy.builder()
                    .title("HIPAA Security & Privacy Standards")
                    .description("US Health Insurance Portability and Accountability Act standards.")
                    .owner(admin)
                    .active(true)
                    .categories(Set.of(catDataPrivacy, catSec))
                    .build());
        }
        final UUID hipaaPolicyId = hipaaPol.getId();

        PolicyVersion hipaaVer = versionRepository.findAll().stream()
                .filter(v -> v.getPolicy().getId().equals(hipaaPolicyId) && Integer.valueOf(1).equals(v.getVersion()))
                .findFirst()
                .orElse(null);

        if (hipaaVer == null) {
            hipaaVer = versionRepository.save(PolicyVersion.builder()
                    .policy(hipaaPol)
                    .version(1)
                    .content(hipaaContent)
                    .status(PolicyVersionStatusEnum.ACTIVE)
                    .approvedBy(admin)
                    .build());
        }

        List<DocumentChunk> hipaaChunks = ingestionService.processAndPersistPolicyVersion(hipaaVer.getId());

        // 5. Expired Legacy Policy
        String legacyContent = """
                # Legacy Data Retention & Archiving Policy
                
                ## Section 9 - Historical Retention Guidelines
                Historical archiving mandates that all operational telemetry, tape backups, and customer logs must be retained for 3 years in physical off-site magnetic tape storage before secure degaussing.
                """;

        Policy legacyPol = policyRepository.findAll().stream()
                .filter(p -> "Legacy Archiving Policy".equals(p.getTitle()))
                .findFirst()
                .orElse(null);

        if (legacyPol == null) {
            legacyPol = policyRepository.save(Policy.builder()
                    .title("Legacy Archiving Policy")
                    .description("Superseded 2019 data retention standard.")
                    .owner(admin)
                    .active(false)
                    .categories(Set.of(catSec))
                    .build());
        }
        final UUID legacyPolicyId = legacyPol.getId();

        PolicyVersion legacyVer = versionRepository.findAll().stream()
                .filter(v -> v.getPolicy().getId().equals(legacyPolicyId) && Integer.valueOf(1).equals(v.getVersion()))
                .findFirst()
                .orElse(null);

        if (legacyVer == null) {
            legacyVer = versionRepository.save(PolicyVersion.builder()
                    .policy(legacyPol)
                    .version(1)
                    .content(legacyContent)
                    .status(PolicyVersionStatusEnum.EXPIRED)
                    .approvedBy(admin)
                    .build());
        }

        List<DocumentChunk> legacyChunks = ingestionService.processAndPersistPolicyVersion(legacyVer.getId());

        int totalChunks = gdprChunks.size() + hipaaChunks.size() + legacyChunks.size();
        log.info("Successfully ingested and chunked sample policies! Total chunks created: {}", totalChunks);
        return totalChunks;
    }
}
