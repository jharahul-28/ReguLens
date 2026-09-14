package com.rj.ReguLens.retrieval.service;

import com.rj.ReguLens.retrieval.model.RetrievalFilter;
import com.rj.ReguLens.retrieval.model.RetrievalResult;

public interface RetrievalService {

    RetrievalResult retrieveRelevantClauses(String queryText, RetrievalFilter filter);
}
