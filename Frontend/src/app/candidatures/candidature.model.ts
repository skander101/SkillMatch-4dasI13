export type CandidatureStatut = 'RECUE' | 'PRESELECTIONNEE' | 'ENTRETIEN_PLANIFIE' | 'ACCEPTEE' | 'REFUSEE' | 'RETIREE';

export interface Candidature {
  id: number;
  offreId: number;
  userId: number;
  cvId: number;
  lettre: string;
  statut: CandidatureStatut;
  dateCandidature: string;
  dateEntretien: string | null;
  noteRecruteur: number | null;
  commentaireRecruteur: string | null;
}

export interface CandidatureCreation {
  offreId: number;
  lettre: string;
  cvId: number;
  userId?: number;
}

// Les champs absents ne sont pas modifiés par le backend.
export interface CandidatureUpdate {
  lettre?: string;
  cvId?: number;
  statut?: CandidatureStatut;
  dateEntretien?: string;
  noteRecruteur?: number;
  commentaireRecruteur?: string;
}
