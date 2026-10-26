import { Routes } from '@angular/router';
import { Events } from './pages/events/events';
import { ManageEvents } from './pages/manage-events/manage-events';
import { EventDetails } from './pages/event-details/event-details';
import { Login } from './pages/login/login';
import { Profile } from './pages/profile/profile';
import { Shell } from './components/shell/shell';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';
import { ADMIN_ROLES, MANAGER_ROLES, MY_EVENTS_ROLES } from './core/services/auth.service';
import { ManageUsers } from './pages/manage-users/manage-users';

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
      { path: 'my-events', component: Events, canActivate: [roleGuard], data: { title: 'My Events', heading: 'Your events', registeredOnly: true, roles: MY_EVENTS_ROLES } },
      { path: 'manage', component: ManageEvents, canActivate: [roleGuard], data: { title: 'Manage Events', roles: MANAGER_ROLES } },
      { path: 'admin/users', component: ManageUsers, canActivate: [roleGuard], data: { title: 'Manage Users', roles: ADMIN_ROLES } },
      { path: 'profile', component: Profile, data: { title: 'Profile' } },
    ],
  },
  { path: '**', redirectTo: 'events' },
];

