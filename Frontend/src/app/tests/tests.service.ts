import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';

export interface TestSummary {
  id: number;
  titre: string;
  competence: string;
  dureeMinutes: number;
  seuilReussite: number;
}

interface TestQuestion {
  id: string;
  enonce: string;
  options: string[];
}

export interface TestDetail extends TestSummary {
  questions: TestQuestion[];
}

export interface TestDefinition extends Omit<TestSummary, 'id'> {
  questions: (TestQuestion & { indexBonneReponse: number })[];
}

export interface TestResult {
  id: number;
  testId: number;
  userId: number;
  competence: string;
  score: number;
  reussi: boolean;
  tentative: number;
  date: string;
}

@Injectable({ providedIn: 'root' })
export class TestsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api`;
  private readonly testsUrl = `${this.apiUrl}/tests`;

  list(competence?: string) {
    const filtre = competence?.trim();
    return this.http.get<TestSummary[]>(this.testsUrl, {
      params: filtre ? { competence: filtre } : {}
    });
  }

  detail(id: number) {
    return this.http.get<TestDetail>(`${this.testsUrl}/${id}`);
  }

  definition(id: number) {
    return this.http.get<TestDefinition>(`${this.testsUrl}/${id}/definition`);
  }

  create(definition: TestDefinition) {
    return this.http.post<TestDetail>(this.testsUrl, definition);
  }

  update(id: number, definition: TestDefinition) {
    return this.http.put<TestDetail>(`${this.testsUrl}/${id}`, definition);
  }

  remove(id: number) {
    return this.http.delete<void>(`${this.testsUrl}/${id}`);
  }

  submit(id: number, reponses: Record<string, number>) {
    return this.http.post<TestResult>(`${this.testsUrl}/${id}/submit`, { reponses });
  }

  myResults() {
    return this.http.get<TestResult[]>(`${this.apiUrl}/results/me`);
  }

  results(id: number) {
    return this.http.get<TestResult[]>(`${this.testsUrl}/${id}/results`);
  }

  check(userId: number, testId: number) {
    return this.http.get<TestResult>(`${this.apiUrl}/results/check`, { params: { userId, testId } });
  }
}

export function testErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504) {
    return 'Le service des tests est indisponible. Réessayez dans un instant.';
  }
  if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous pour continuer.';
  if (error.status === 403) return 'Votre compte ne permet pas cette action.';

  const detail = error.error?.detail ?? error.error?.message;
  return typeof detail === 'string' && detail.trim() ? detail : fallback;
}
