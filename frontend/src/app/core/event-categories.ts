export const EVENT_CATEGORIES = ['Technology', 'Social', 'Workshops', 'Volunteering'] as const;
export type EventCategory = (typeof EVENT_CATEGORIES)[number];
export const ALL_EVENTS_FILTER = 'All events';
