-- V3__create_audit_and_evaluation_tables.sql: Structured Decision Logging & Evaluation Harness

-- Query Audit & Decision Traceability Log
CREATE TABLE IF NOT EXISTS query_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    query_text TEXT NOT NULL,
    retrieved_chunks JSONB NOT NULL DEFAULT '[]'::jsonb,
    similarity_threshold NUMERIC(4,3) NOT NULL,
    raw_prompt TEXT NOT NULL,
    generated_answer TEXT NOT NULL,
    structured_citations JSONB NOT NULL DEFAULT '[]'::jsonb,
    groundedness_status VARCHAR(50) NOT NULL, -- 'VERIFIED_ENTAILED', 'VERIFICATION_FAILED', 'THRESHOLD_REJECTED'
    model_identifier VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    retrieval_latency_ms BIGINT NOT NULL DEFAULT 0,
    generation_latency_ms BIGINT NOT NULL DEFAULT 0,
    total_latency_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_query_audit_user_date ON query_audit_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_query_audit_groundedness ON query_audit_log(groundedness_status);

-- Evaluation Benchmark Golden Dataset
CREATE TABLE IF NOT EXISTS evaluation_benchmark_set (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query_text TEXT NOT NULL,
    expected_clause_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    ground_truth_answer TEXT NOT NULL,
    jurisdiction VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Evaluation Run Summary Results
CREATE TABLE IF NOT EXISTS evaluation_run_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    total_queries INT NOT NULL,
    hit_rate_at_5 NUMERIC(5,2) NOT NULL,
    mean_reciprocal_rank NUMERIC(5,4) NOT NULL,
    precision_at_3 NUMERIC(5,2) NOT NULL,
    faithfulness_rate NUMERIC(5,2) NOT NULL,
    avg_total_latency_ms BIGINT NOT NULL,
    benchmark_report JSONB NOT NULL DEFAULT '{}'::jsonb
);
