import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { Candidature, CandidatureCreation, CandidatureUpdate } from './candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly http = inject(HttpClient); // Le JWT est ajouté par l'intercepteur existant.
  private readonly url = environment.apiUrl + '/skillmatch/candidatures';

  lister(search = '', sort = 'dateCandidatureDesc') {
    const params = new HttpParams().set('search', search).set('sort', sort);
    return this.http.get<Candidature[]>(this.url, { params });
  }

  creer(demande: CandidatureCreation) {
    return this.http.post<Candidature>(this.url, demande);
  }

  modifier(id: number, demande: CandidatureUpdate) {
    return this.http.put<Candidature>(this.url + '/' + id, demande);
  }

  supprimer(id: number) { // Retrait pour CANDIDAT, suppression définitive pour ADMIN.
    return this.http.delete<void>(this.url + '/' + id);
  }
}
