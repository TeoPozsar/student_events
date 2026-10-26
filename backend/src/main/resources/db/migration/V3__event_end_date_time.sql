ALTER TABLE events ADD COLUMN end_date_time TIMESTAMP;

-- Existing events had no end time; assume a two-hour duration.
UPDATE events SET end_date_time = start_date_time + INTERVAL '2 hours';

ALTER TABLE events ALTER COLUMN end_date_time SET NOT NULL;
ALTER TABLE events ADD CONSTRAINT chk_events_end_after_start CHECK (end_date_time > start_date_time);

CREATE INDEX idx_events_end_date_time ON events(end_date_time);
CREATE INDEX idx_events_organizer_id ON events(organizer_id);
