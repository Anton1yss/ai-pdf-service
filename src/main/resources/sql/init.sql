CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(128) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pdf_files (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    original_s3_key VARCHAR(512),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version VARCHAR(32) DEFAULT 1,
    user_id  BIGINT REFERENCES users(id) ON DELETE CASCADE,
    parent_file_id BIGINT REFERENCES pdf_files(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS processing_jobs(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    file_id BIGINT REFERENCES pdf_files(id),
    action TEXT NOT NULL,
    response_message TEXT,
    result_s3_key VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    file_id BIGINT REFERENCES pdf_files(id) ON DELETE SET NULL,
    processing_job_id BIGINT REFERENCES processing_jobs(id) ON DELETE SET NULL,
    action VARCHAR(32) NOT NULL,
    message TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

drop table audit_logs;
drop table processing_jobs;
drop table pdf_files;
drop table users;
