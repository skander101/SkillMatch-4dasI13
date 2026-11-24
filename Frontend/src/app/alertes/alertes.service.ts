import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Alerte, AlerteRequest, Favori } from './alertes.model';

/** Appels vers alertes-service via l'API Gateway (le JWT est ajouté par authInterceptor). */
@Injectable({ providedIn: 'root' })
export class AlertesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly alertsUrl = `${this.apiUrl}/skillmatch/alerts`;
  private readonly favoritesUrl = `${this.apiUrl}/skillmatch/favorites`;

  mesAlertes() {
    return this.http.get<Alerte[]>(`${this.alertsUrl}/me`);
  }

  creerAlerte(request: AlerteRequest) {
    return this.http.post<Alerte>(this.alertsUrl, request);
  }

  modifierAlerte(id: number, request: AlerteRequest) {
    return this.http.put<Alerte>(`${this.alertsUrl}/${id}`, request);
  }

  changerActive(id: number, active: boolean) {
    return this.http.put<Alerte>(`${this.alertsUrl}/${id}/active`, { active });
  }

  supprimerAlerte(id: number) {
    return this.http.delete<void>(`${this.alertsUrl}/${id}`);
  }

  mesFavoris() {
    return this.http.get<Favori[]>(`${this.favoritesUrl}/me`);
  }

  ajouterFavori(offerId: number) {
    return this.http.post<Favori>(this.favoritesUrl, { offerId });
  }

  retirerFavori(id: number) {
    return this.http.delete<void>(`${this.favoritesUrl}/${id}`);
  }
}
