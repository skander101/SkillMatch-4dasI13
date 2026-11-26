import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateProfilRequest, Profil, ProfilCompleteness, ProfilUpdate } from './profil.model';

@Injectable({ providedIn: 'root' })
export class ProfilService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/skillmatch/profiles`;

  getMe(): Observable<Profil> {
    return this.http.get<Profil>(`${this.baseUrl}/me`);
  }

  listAll(): Observable<Profil[]> {
    return this.http.get<Profil[]>(this.baseUrl);
  }

  getById(userId: number): Observable<Profil> {
    return this.http.get<Profil>(`${this.baseUrl}/${userId}`);
  }

  deleteByAdmin(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}`);
  }

  updateMe(update: ProfilUpdate): Observable<Profil> {
    return this.http.put<Profil>(`${this.baseUrl}/me`, update);
  }

  create(request: CreateProfilRequest): Observable<Profil> {
    return this.http.post<Profil>(this.baseUrl, request);
  }

  getCompleteness(): Observable<ProfilCompleteness> {
    return this.http.get<ProfilCompleteness>(`${this.baseUrl}/me/completeness`);
  }
}
