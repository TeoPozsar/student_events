import { Component, computed, input } from '@angular/core';
import { EventStatus } from '../../core/services/event.service';

const LABELS: Record<EventStatus, string> = {
  UPCOMING: 'Upcoming',
  IN_PROGRESS: 'In Progress',
  FINISHED: 'Finished',
};

@Component({
  selector: 'app-status-badge',
  template: `<span class="status-badge" [attr.data-status]="status()">{{ label() }}</span>`,
  styleUrl: './status-badge.scss',
})
export class StatusBadge {
  status = input.required<EventStatus>();
  label = computed(() => LABELS[this.status()] ?? this.status());
}
