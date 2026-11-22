import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface ChoixReference { id: number; nom: string; }
export interface ReferencesOffre { entreprises: ChoixReference[]; tests: ChoixReference[]; }

@Injectable({ providedIn: 'root' })
export class ReferencesService {
  private readonly http = inject(HttpClient);
  // Catalogue local de démonstration ; les futurs services fourniront ces choix.
  charger() { return this.http.get<ReferencesOffre>('/demo/references.json'); }
}
