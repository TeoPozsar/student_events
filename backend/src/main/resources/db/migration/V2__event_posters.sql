ALTER TABLE events
    ADD COLUMN poster_updated_at TIMESTAMP;

CREATE TABLE event_posters (
    event_id BIGINT PRIMARY KEY REFERENCES events(id) ON DELETE CASCADE,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL
);
