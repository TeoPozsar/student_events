-- Only fixes case/whitespace variants of the supported categories.
-- Any other legacy value is left untouched so no data is lost.
UPDATE events SET category = 'Technology' WHERE LOWER(TRIM(category)) IN ('technology', 'tech') AND category <> 'Technology';
UPDATE events SET category = 'Social' WHERE LOWER(TRIM(category)) = 'social' AND category <> 'Social';
UPDATE events SET category = 'Workshops' WHERE LOWER(TRIM(category)) IN ('workshops', 'workshop') AND category <> 'Workshops';
UPDATE events SET category = 'Volunteering' WHERE LOWER(TRIM(category)) IN ('volunteering', 'volunteer') AND category <> 'Volunteering';
