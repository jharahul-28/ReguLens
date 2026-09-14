package com.rj.ReguLens.generation.service;

import com.rj.ReguLens.generation.model.ComplianceAnswer;
import com.rj.ReguLens.retrieval.model.RetrievalFilter;

public interface ComplianceGenerationService {

    ComplianceAnswer answerComplianceQuery(String query, RetrievalFilter filter);
}
