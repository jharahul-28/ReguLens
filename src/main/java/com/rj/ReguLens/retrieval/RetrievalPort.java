package com.rj.ReguLens.retrieval;

import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.ScoredDocumentChunk;

import java.util.List;

/**
 * Driven port for vector similarity and hybrid search retrieval.
 */
public interface RetrievalPort {

    List<ScoredDocumentChunk> searchVector(float[] queryVector, RetrievalFilter filter);

    List<ScoredDocumentChunk> searchHybrid(float[] queryVector, String queryText, RetrievalFilter filter);
}
