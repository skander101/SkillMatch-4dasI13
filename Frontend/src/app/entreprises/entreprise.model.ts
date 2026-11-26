export type StatutModeration = 'EN_ATTENTE' | 'APPROUVE' | 'REJETE';

export interface Entreprise {
  id: number;
  nom: string;
  secteur: string;
  ville: string;
  description: string;
  noteMoyenne: number;
}

export interface Avis {
  id: number;
  entrepriseId: number;
  utilisateurId: number;
  nomUtilisateur: string;
  note: number;
  commentaire: string;
  statut: StatutModeration;
  dateCreation: string;
}

export interface CreateEntrepriseRequest {
  nom: string;
  secteur: string;
  ville: string;
  description: string;
}

export type UpdateEntrepriseRequest = CreateEntrepriseRequest;

export interface SubmitAvisRequest {
  note: number;
  commentaire: string;
}

export interface ModerationRequest {
  statut: 'APPROUVE' | 'REJETE';
}
