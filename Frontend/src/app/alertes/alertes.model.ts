export type ContratType = 'CDI' | 'CDD' | 'STAGE' | 'FREELANCE';
export type Frequence = 'IMMEDIATE' | 'QUOTIDIENNE' | 'HEBDOMADAIRE';

export interface Alerte {
  id: number;
  userId: number;
  motsCles: string[];
  localisation: string | null;
  contrat: ContratType | null;
  frequence: Frequence;
  active: boolean;
  dateCreation: string;
}

export interface AlerteRequest {
  motsCles: string[];
  localisation?: string | null;
  contrat?: ContratType | null;
  frequence?: Frequence | null;
}

export interface Favori {
  id: number;
  userId: number;
  offerId: number;
  date: string;
}
