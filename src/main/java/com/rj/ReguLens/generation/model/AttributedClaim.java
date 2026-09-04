package com.rj.ReguLens.generation.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AttributedClaim {
    String statement;
    UUID citedChunkId;
    String clauseReference;
    boolean verified;
    String verificationReason;
}
