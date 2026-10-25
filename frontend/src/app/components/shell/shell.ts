import { Component, DestroyRef, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { Navbar } from '../navbar/navbar';
import { UserMenu } from '../user-menu/user-menu';
import { AuthService } from '../../core/services/auth.service';
import { NoticeService } from '../../core/services/notice.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, Navbar, UserMenu],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  readonly authService = inject(AuthService);
  readonly notice = inject(NoticeService);
  private readonly router = inject(Router);

  readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.currentTitle()),
      startWith(this.currentTitle()),
    ),
    { initialValue: '' },
  );

  constructor() {
    // Instantiating the profile service starts loading the signed-in user's profile picture.
    inject(ProfileService);
    inject(DestroyRef).onDestroy(() => this.notice.clear());
  }

  private currentTitle(): string {
    let route = this.router.routerState.snapshot.root;
    let title = '';
    while (route) {
      title = route.data['title'] ?? title;
      route = route.firstChild as typeof route;
    }
    return title;
  }
}
