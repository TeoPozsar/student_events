import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TOAST_DURATION_MS, TOAST_FADE_MS, ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = TestBed.inject(ToastService);
  });

  afterEach(() => vi.useRealTimers());

  it('shows a toast and removes it after 3 seconds', () => {
    service.success('Done');
    expect(service.toasts().length).toBe(1);

    vi.advanceTimersByTime(TOAST_DURATION_MS);
    expect(service.toasts()[0].leaving).toBe(true);

    vi.advanceTimersByTime(TOAST_FADE_MS);
    expect(service.toasts().length).toBe(0);
  });

  it('ignores an identical message that is still showing', () => {
    service.success('Done');
    service.success('Done');
    expect(service.toasts().length).toBe(1);
  });
});
