import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Avis, CreateEntrepriseRequest, Entreprise, ModerationRequest, SubmitAvisRequest, UpdateEntrepriseRequest } from './entreprise.model';

@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly entreprisesUrl = `${this.apiUrl}/skillmatch/entreprises`;
  private readonly avisUrl = `${this.apiUrl}/skillmatch/avis`;

  list() {
    return this.http.get<Entreprise[]>(this.entreprisesUrl);
  }

  get(id: number) {
    return this.http.get<Entreprise>(`${this.entreprisesUrl}/${id}`);
  }

  create(request: CreateEntrepriseRequest) {
    return this.http.post<Entreprise>(this.entreprisesUrl, request);
  }

  update(id: number, request: UpdateEntrepriseRequest) {
    return this.http.put<Entreprise>(`${this.entreprisesUrl}/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.entreprisesUrl}/${id}`);
  }

  listAvis(id: number) {
    return this.http.get<Avis[]>(`${this.entreprisesUrl}/${id}/avis`);
  }

  submitAvis(id: number, request: SubmitAvisRequest) {
    return this.http.post<Avis>(`${this.entreprisesUrl}/${id}/avis`, request);
  }

  moderateAvis(id: number, request: ModerationRequest) {
    return this.http.put<Avis>(`${this.avisUrl}/${id}/moderation`, request);
  }

  deleteAvis(id: number) {
    return this.http.delete<void>(`${this.avisUrl}/${id}`);
  }
}
