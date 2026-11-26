import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cv } from './cv.model';

@Injectable({ providedIn: 'root' })
export class CvService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/skillmatch/profiles/me/cv`;
  private readonly allUrl = `${environment.apiUrl}/skillmatch/profiles/cvs`;

  listAll(): Observable<Cv[]> {
    return this.http.get<Cv[]>(this.allUrl);
  }

  deleteByAdmin(cvId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/skillmatch/profiles/cv/${cvId}`);
  }

  list(): Observable<Cv[]> {
    return this.http.get<Cv[]>(this.baseUrl);
  }

  upload(file: File): Observable<Cv> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<Cv>(this.baseUrl, form);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
