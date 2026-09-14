package com.rj.ReguLens.generation.verifier;

import com.rj.ReguLens.generation.model.GroundednessStatus;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroundednessVerifierTest {

    private GroundednessVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new GroundednessVerifier();
    }

    @Test
    void shouldVerifyEntailedClaimsWithValidCitations() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .clauseReference("Article 17(1)")
                .content("The data subject shall have the right to obtain erasure of personal data without undue delay.")
                .build();

        String answer = "Data subjects possess the formal legal right to obtain the erasure of personal data [Ref: " + chunkId + "]. "
                + "Controllers must execute data erasure without undue delay [Ref: " + chunkId + "].";

        GroundednessVerifier.VerificationResult result = verifier.verify(answer, List.of(chunk));

        assertNotNull(result);
        assertEquals(GroundednessStatus.VERIFIED_ENTAILED, result.getStatus());
        assertTrue(result.getConfidence() >= 0.80);
        assertEquals(2, result.getClaims().size());
        assertTrue(result.getClaims().get(0).isVerified());
    }

    @Test
    void shouldFailVerificationWhenClaimsLackCitationsOrHallucinate() {
        UUID chunkId = UUID.randomUUID();
        ScoredDocumentChunk chunk = ScoredDocumentChunk.builder()
                .chunkId(chunkId)
                .clauseReference("Article 17")
                .content("Erasure of personal data.")
                .build();

        String answer = "Organizations must also pay mandatory quarterly crypto fines to the regulator. "
                + "Data subjects can request deletion [Ref: " + UUID.randomUUID() + "]."; // Invalid chunk UUID

        GroundednessVerifier.VerificationResult result = verifier.verify(answer, List.of(chunk));

        assertEquals(GroundednessStatus.VERIFICATION_FAILED, result.getStatus());
        assertTrue(result.getConfidence() < 0.80);
    }
}
