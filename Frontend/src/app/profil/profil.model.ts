export interface Profil {
  id: number;
  userId: number;
  titre: string | null;
  localisation: string | null;
  competences: string[];
  experiences: string[];
  badges: string[];
  completude: number;
}

export interface ProfilUpdate {
  titre: string | null;
  localisation: string | null;
  competences: string[];
  experiences: string[];
  badges: string[];
}

export interface CreateProfilRequest extends ProfilUpdate {
  userId: number;
}

export interface ProfilCompleteness {
  userId: number;
  completude: number;
  champsManquants: string[];
}
