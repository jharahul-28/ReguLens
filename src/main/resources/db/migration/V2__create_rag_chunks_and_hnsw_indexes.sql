-- V2__create_rag_chunks_and_hnsw_indexes.sql: Vector & Semantic Chunk Storage for Neon pgvector

CREATE EXTENSION IF NOT EXISTS vector;

-- Document Chunk Table
CREATE TABLE IF NOT EXISTS document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_version_id UUID NOT NULL REFERENCES policy_version(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    clause_reference VARCHAR(255) NOT NULL,
    section_hierarchy TEXT NOT NULL,
    content TEXT NOT NULL,
    token_count INT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    content_tsv tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_policy_version_chunk UNIQUE (policy_version_id, chunk_index)
);

-- HNSW Vector Cosine Index for Sub-30ms Semantic Retrieval
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding_hnsw 
ON document_chunk USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- GIN Index for Fast Full-Text Hybrid Search
CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING gin(content_tsv);

-- GIN Index for Metadata Pre-Filtering (Jurisdiction, Regulation, Effective Date)
CREATE INDEX IF NOT EXISTS idx_document_chunk_metadata ON document_chunk USING gin(metadata);
