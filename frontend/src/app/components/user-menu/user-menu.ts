import { Component, ElementRef, HostListener, inject, signal, viewChild, viewChildren } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'app-user-menu',
  templateUrl: './user-menu.html',
  styleUrl: './user-menu.scss',
})
export class UserMenu {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly profileService = inject(ProfileService);

  readonly open = signal(false);
  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');
  private readonly items = viewChildren<ElementRef<HTMLButtonElement>>('item');

  toggle(): void {
    this.open() ? this.close() : this.show();
  }

  show(focusIndex?: number): void {
    this.open.set(true);
    if (focusIndex !== undefined) {
      // Wait for the menu to render before moving focus into it.
      queueMicrotask(() => setTimeout(() => this.focusItem(focusIndex)));
    }
  }

  close(returnFocus = false): void {
    this.open.set(false);
    if (returnFocus) this.trigger().nativeElement.focus();
  }

  openProfile(): void {
    this.close();
    this.router.navigate(['/profile']);
  }

  logout(): void {
    this.close();
    this.authService.logout();
  }

  onTriggerKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      this.show(event.key === 'ArrowDown' ? 0 : 1);
    }
  }

  onMenuKeydown(event: KeyboardEvent): void {
    const items = this.items();
    const current = items.findIndex((item) => item.nativeElement === document.activeElement);
    switch (event.key) {
      case 'ArrowDown': event.preventDefault(); this.focusItem((current + 1) % items.length); break;
      case 'ArrowUp': event.preventDefault(); this.focusItem((current - 1 + items.length) % items.length); break;
      case 'Home': event.preventDefault(); this.focusItem(0); break;
      case 'End': event.preventDefault(); this.focusItem(items.length - 1); break;
      case 'Tab': this.close(); break;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) this.close(true);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.close();
  }

  private focusItem(index: number): void {
    this.items()[index]?.nativeElement.focus();
  }
}
