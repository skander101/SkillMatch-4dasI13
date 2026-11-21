import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Offre, OffreRequest } from './offre.model';

@Injectable({ providedIn: 'root' })
export class OffreService {
  private readonly http = inject(HttpClient); // Réutilise l'intercepteur JWT existant.
  private readonly url = environment.apiUrl + '/skillmatch/offres';

  lister(search: string, sort: string, personnelles = false) {
    const params = new HttpParams().set('search', search).set('sort', sort);
    return this.http.get<Offre[]>(this.url + (personnelles ? '/mes-offres' : ''), { params });
  }

  creer(demande: OffreRequest) {
    return this.http.post<Offre>(this.url, demande);
  }

  modifier(id: number, demande: OffreRequest) {
    return this.http.put<Offre>(this.url + '/' + id, demande);
  }

  supprimer(id: number) {
    return this.http.delete<void>(this.url + '/' + id);
  }
}
