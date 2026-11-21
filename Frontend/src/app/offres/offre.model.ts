// Même contrat JSON que le backend.
export type ContratType = 'CDI' | 'CDD' | 'STAGE' | 'FREELANCE';
export type OffreStatut = 'BROUILLON' | 'PUBLIEE' | 'FERMEE' | 'EXPIREE';

export interface Offre {
  id: number;
  titre: string;
  description: string | null;
  competences: string[];
  localisation: string | null;
  contrat: ContratType;
  salaire: number | null;
  dateExpiration: string;
  statut: OffreStatut;
  entrepriseId: number | null;
  testRequisId: number | null;
  recruteurId: number;
}

// Le backend attribue l'id ; le propriétaire vient de la session recruteur.
export type OffreRequest = Omit<Offre, 'id' | 'recruteurId'> & { recruteurId?: number | null };
