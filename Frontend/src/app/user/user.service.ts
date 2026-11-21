import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { CreateUserRequest, UpdateUserRequest, User } from './user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly usersUrl = `${this.apiUrl}/skillmatch/utilisateurs`;

  list() {
    return this.http.get<User[]>(this.usersUrl);
  }

  create(request: CreateUserRequest) {
    return this.http.post<User>(this.usersUrl, request);
  }

  update(id: number, request: UpdateUserRequest) {
    return this.http.put<User>(`${this.usersUrl}/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.usersUrl}/${id}`);
  }
}
