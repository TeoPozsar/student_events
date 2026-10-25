import { Routes } from '@angular/router';
import { Events } from './pages/events/events';
import { ManageEvents } from './pages/manage-events/manage-events';
import { EventDetails } from './pages/event-details/event-details';
import { Login } from './pages/login/login';
import { Profile } from './pages/profile/profile';
import { Shell } from './components/shell/shell';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';
import { MANAGER_ROLES } from './core/services/auth.service';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [guestGuard], data: { mode: 'login' } },
  { path: 'register', component: Login, canActivate: [guestGuard], data: { mode: 'register' } },
  {
    path: '',
    component: Shell,
    canActivateChild: [authGuard],
    children: [
      { path: '', redirectTo: 'events', pathMatch: 'full' },
      { path: 'events', component: Events, data: { title: 'Discover Events', heading: 'Upcoming on campus' } },
      { path: 'events/:id', component: EventDetails, data: { title: 'Event Details' } },
      { path: 'my-events', component: Events, data: { title: 'My Events', heading: 'Your events', registeredOnly: true } },
      { path: 'manage', component: ManageEvents, canActivate: [roleGuard], data: { title: 'Manage Events', roles: MANAGER_ROLES } },
      { path: 'profile', component: Profile, data: { title: 'Profile' } },
    ],
  },
  { path: '**', redirectTo: 'events' },
];

