import { HttpErrorResponse } from '@angular/common/http';

export interface FriendlyError {
  message: string;
  fieldErrors: Record<string, string>;
}

interface ApiErrorBody {
  message?: string;
  fieldErrors?: Record<string, string>;
}

// Turns any HTTP failure into a message that is safe to show to users.
// Server-side details (stack traces, 5xx bodies) are never surfaced.
export function toFriendlyError(error: unknown, fallback = 'Something went wrong. Please try again.'): FriendlyError {
  if (!(error instanceof HttpErrorResponse)) {
    return { message: fallback, fieldErrors: {} };
  }

  const body: ApiErrorBody | null = typeof error.error === 'object' && error.error ? error.error : null;
  const fieldErrors = body?.fieldErrors ?? {};

  if (error.status === 0) {
    return { message: 'Cannot reach the server. Check your connection and try again.', fieldErrors: {} };
  }
  if (error.status >= 500) {
    return { message: 'The server ran into a problem. Please try again in a moment.', fieldErrors: {} };
  }
  if (error.status === 400 && Object.keys(fieldErrors).length > 0) {
    return { message: 'Please correct the highlighted fields.', fieldErrors };
  }
  if (error.status === 403) {
    return { message: 'You do not have permission to do that.', fieldErrors: {} };
  }
  if (error.status === 413) {
    return { message: 'The uploaded file is too large (max 5 MB).', fieldErrors: {} };
  }
  if (error.status === 415) {
    return { message: 'Please choose a JPEG, PNG or WEBP image.', fieldErrors: {} };
  }

  return { message: body?.message ?? fallback, fieldErrors };
}
