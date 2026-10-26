import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { AuthService, Role } from '../../core/services/auth.service';
import { ManagedUser, UserAdminService } from '../../core/services/user-admin.service';
import { toFriendlyError } from '../../core/utils/http-error';
import { ToastService } from '../../core/services/toast.service';

interface RoleChange {
  user: ManagedUser;
  role: Role;
}

@Component({
  selector: 'app-manage-users',
  templateUrl: './manage-users.html',
  styleUrl: './manage-users.scss',
})
export class ManageUsers {
  private readonly userAdmin = inject(UserAdminService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);

  readonly roles: Role[] = ['USER', 'ORGANIZER', 'ADMIN'];
  readonly users = signal<ManagedUser[]>([]);
  readonly search = signal('');
  readonly roleFilter = signal<Role | 'ALL'>('ALL');
  readonly filteredUsers = computed(() => {
    const terms = this.search().trim().toLowerCase().split(/\s+/).filter(Boolean);
    const role = this.roleFilter();
    return this.users().filter((user) => {
      if (role !== 'ALL' && user.role !== role) return false;
      const haystack = `${user.firstName} ${user.lastName}`.toLowerCase();
      return terms.every((term) => haystack.includes(term));
    });
  });
  readonly hasFilters = computed(() => this.search().trim() !== '' || this.roleFilter() !== 'ALL');
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly saving = signal(false);
  readonly pending = signal<RoleChange | null>(null);
  readonly selections = signal<Record<number, Role>>({});
  readonly toast = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly currentUserId = computed(() => this.auth.user()?.id ?? null);

  private readonly confirmDialog = viewChild<ElementRef<HTMLDialogElement>>('confirmDialog');
  private toastTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.userAdmin.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (users) => {
        this.users.set(users);
        this.selections.set({});
      },
      error: (error) => this.errorMessage.set(toFriendlyError(error, 'Could not load users. Please try again.').message),
    });
  }

  isSelf(user: ManagedUser): boolean {
    return user.id === this.currentUserId();
  }

  selectedRole(user: ManagedUser): Role {
    return this.selections()[user.id] ?? user.role;
  }

  hasChange(user: ManagedUser): boolean {
    return this.selectedRole(user) !== user.role;
  }

  select(user: ManagedUser, value: string): void {
    this.selections.update((current) => ({ ...current, [user.id]: value as Role }));
  }

  roleLabel(role: string): string {
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  setRoleFilter(value: string): void {
    this.roleFilter.set(this.roles.includes(value as Role) ? (value as Role) : 'ALL');
  }

  clearFilters(): void {
    this.search.set('');
    this.roleFilter.set('ALL');
  }

  askConfirm(user: ManagedUser): void {
    if (this.isSelf(user) || !this.hasChange(user)) return;
    this.pending.set({ user, role: this.selectedRole(user) });
    this.confirmDialog()?.nativeElement.showModal();
  }

  cancelConfirm(): void {
    if (this.saving()) return;
    this.closeConfirm();
  }

  confirm(): void {
    const change = this.pending();
    if (!change || this.saving()) return;
    this.saving.set(true);
    this.userAdmin.updateRole(change.user.id, change.role).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.selections.update(({ [updated.id]: _removed, ...rest }) => rest);
        this.closeConfirm();
        this.toasts.success('User role updated successfully!');
      },
      error: (error) => {
        this.closeConfirm();
        this.showToast('error', toFriendlyError(error, 'Could not update the role. Please try again.').message);
      },
    });
  }

  dismissToast(): void {
    this.toast.set(null);
  }

  private closeConfirm(): void {
    this.confirmDialog()?.nativeElement.close();
    this.pending.set(null);
  }

  private showToast(type: 'success' | 'error', text: string): void {
    clearTimeout(this.toastTimer);
    this.toast.set({ type, text });
    this.toastTimer = setTimeout(() => this.toast.set(null), 5000);
  }
}
