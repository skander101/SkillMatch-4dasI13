export interface NavService {
  id: string;
  label: string;
  full: string;
  items: string[];
}

export interface AppNotification {
  tag: string;
  title: string;
  detail: string;
  time: string;
}

export const NAV_SERVICES: NavService[] = [
  { id: 'offres', label: 'Offres', full: 'Offres & Candidatures', items: ['Rechercher une offre', 'Offres recommandées', 'Déposer une candidature', 'Suivre mes candidatures', 'Planifier un entretien'] },
  { id: 'profils', label: 'Profils', full: 'Profils & CV', items: ['Compléter mon profil', 'Compétences & badges', 'Créer mon CV', 'Complétude du profil'] },
  { id: 'entreprises', label: 'Entreprises', full: 'Entreprises & Avis', items: ['Annuaire des entreprises', 'Publier un avis', 'Consulter les évaluations', 'Modérer les avis'] },
  { id: 'tests', label: 'Tests', full: 'Tests & Compétences', items: ['Passer un test technique', 'Correction automatique', 'Mes résultats', 'Tentatives restantes'] },
  { id: 'alertes', label: 'Alertes', full: 'Alertes & Favoris', items: ['Créer une alerte emploi', 'Gérer mes alertes', 'Sauvegarder une offre', 'Mes favoris'] },
  { id: 'messagerie', label: 'Messagerie', full: 'Messagerie', items: ['Conversations', 'Envoyer un message', 'Historique des messages', 'Messagerie recruteur'] }
];

export const NOTIFICATIONS: AppNotification[] = [
  { tag: 'CANDIDATURE', title: 'Candidature acceptée', detail: 'TechNova · Développeur Full Stack', time: 'il y a 2 h' },
  { tag: 'TEST', title: 'Compétence validée', detail: 'Test Angular réussi · badge obtenu', time: 'hier' },
  { tag: 'ALERTE', title: 'Nouvelle offre correspondante', detail: 'Data Analyst · Sfax', time: 'hier' },
  { tag: 'MESSAGE', title: 'Nouveau message', detail: 'Talent RH vous a écrit', time: 'il y a 3 j' }
];
