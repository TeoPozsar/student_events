import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Role } from './auth.service';

export interface ManagedUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8081/api/admin/users';

  list(): Observable<ManagedUser[]> {
    return this.http.get<ManagedUser[]>(this.baseUrl);
  }

  updateRole(userId: number, role: Role): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.baseUrl}/${userId}/role`, { role });
  }
}
