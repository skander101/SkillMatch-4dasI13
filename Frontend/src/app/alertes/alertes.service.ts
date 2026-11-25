import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Alerte, AlerteRequest, Favori } from './alertes.model';

/** Appels vers alertes-service via l'API Gateway (le JWT est ajouté par authInterceptor). */
@Injectable({ providedIn: 'root' })
export class AlertesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly alertsUrl = `${this.apiUrl}/skillmatch/alerts`;
  private readonly favoritesUrl = `${this.apiUrl}/skillmatch/favorites`;

  /** Favoris du candidat connecté, partagés entre la liste des offres (bouton ♡) et la page « Mes favoris ». */
  readonly favoris = signal<Favori[]>([]);
  readonly favoriOfferIds = computed(() => this.favoris().map((favori) => favori.offerId));

  // ---------- Front office : alertes (rôle CANDIDAT) ----------

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

  // ---------- Front office : favoris (rôle CANDIDAT) ----------

  mesFavoris() {
    return this.http.get<Favori[]>(`${this.favoritesUrl}/me`);
  }

  ajouterFavori(offerId: number) {
    return this.http.post<Favori>(this.favoritesUrl, { offerId });
  }

  retirerFavori(id: number) {
    return this.http.delete<void>(`${this.favoritesUrl}/${id}`);
  }

  /** Recharge les favoris du candidat connecté dans le signal partagé. */
  chargerFavoris(): Observable<Favori[]> {
    return this.mesFavoris().pipe(tap((favoris) => this.favoris.set(favoris)));
  }

  /** Ajoute ou retire l'offre des favoris. Renvoie true si l'offre vient d'être ajoutée. */
  basculerFavori(offerId: number): Observable<boolean> {
    const existant = this.favoris().find((favori) => favori.offerId === offerId);
    if (existant) {
      return this.retirerFavori(existant.id).pipe(
        tap(() => this.favoris.update((liste) => liste.filter((favori) => favori.id !== existant.id))),
        map(() => false)
      );
    }
    return this.ajouterFavori(offerId).pipe(
      tap((favori) => this.favoris.update((liste) => [favori, ...liste])),
      map(() => true)
    );
  }

  /** Retire un favori par son id et met à jour le signal partagé. */
  retirerFavoriEtActualiser(id: number) {
    return this.retirerFavori(id).pipe(
      tap(() => this.favoris.update((liste) => liste.filter((favori) => favori.id !== id)))
    );
  }

  viderFavoris(): void {
    this.favoris.set([]);
  }

  // ---------- Back office (rôle ADMIN) ----------

  toutesLesAlertes() {
    return this.http.get<Alerte[]>(`${this.alertsUrl}/admin`);
  }

  adminChangerActive(id: number, active: boolean) {
    return this.http.put<Alerte>(`${this.alertsUrl}/admin/${id}/active`, { active });
  }

  adminSupprimerAlerte(id: number) {
    return this.http.delete<void>(`${this.alertsUrl}/admin/${id}`);
  }

  tousLesFavoris() {
    return this.http.get<Favori[]>(`${this.favoritesUrl}/admin`);
  }

  adminSupprimerFavori(id: number) {
    return this.http.delete<void>(`${this.favoritesUrl}/admin/${id}`);
  }
}

/** Message lisible à partir d'une erreur HTTP de alertes-service ({status, message}). */
export function alertesErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'alertes-service est injoignable (vérifier la gateway et le port 8085).';
    }
    const body = error.error as { message?: string; detail?: string } | null;
    return body?.message ?? body?.detail ?? fallback;
  }
  return fallback;
}
