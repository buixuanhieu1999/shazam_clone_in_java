CREATE TABLE IF NOT EXISTS songs (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255),
    artist VARCHAR(255),
    file_path VARCHAR(500),
    duration DOUBLE
);

CREATE TABLE IF NOT EXISTS fingerprints (
    hash BIGINT,
    time_offset INT,
    song_id VARCHAR(36),
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    INDEX idx_hash (hash)
);
