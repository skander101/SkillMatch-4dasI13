export interface ParticipantSummary {
  id: number;
  nom: string;
  role: string | null;
}

export interface UserSummary {
  id: number;
  nom: string;
  role: string | null;
  /** Nom complet affiché dans les suggestions (ex. "Sara Baker") */
  fullName?: string;
  /** Nom court affiché dans les puces (ex. "Sara B.") */
  displayName?: string;
  /** Initiales calculées à partir du vrai nom (ex. "SB") */
  initials?: string;
}

export type ConversationType = 'direct' | 'groupe';

export type MessageType = 'texte' | 'systeme' | 'fichier';

export interface Conversation {
  id: string;
  participantIds: number[];
  candidatUserId: number;
  nomGroupe?: string | null;
  type: ConversationType;
  estGroupe?: boolean;
  dateCreation?: string | null;
  dernierMessageAt: string | null;
  dernierMessageContenu?: string | null;
  dernierMessageSenderId?: number | null;
  dernierMessageSenderNom?: string | null;
  nonLusCount?: number;
  participants?: ParticipantSummary[];
}

export interface Message {
  id: string;
  contenu: string;
  type: MessageType;
  date: string;
  lu: boolean;
  conversationId: string;
  senderId: number;
  senderNom?: string | null;
  fichierNom?: string | null;
  fichierUrl?: string | null;
  fichierTaille?: number | null;
  signalee?: boolean;
  masque?: boolean;
}

export type ConversationFilterKey = 'all' | 'unread' | 'recent';

export type SignalementMotif = 'Langage inapproprié' | 'Spam' | 'Harcèlement' | 'Autre';

export type SignalementActionAdmin = 'IGNORER' | 'AVERTIR' | 'MASQUER_MESSAGE' | 'SUSPENDRE_ENVOI';

export interface SignalementItem {
  id: string;
  conversationId: string;
  messageId: string;
  auteurSignalementNom: string;
  auteurSignalementRole: string | null;
  auteurMessageNom: string;
  auteurMessageRole: string | null;
  motif: string;
  commentaire: string | null;
  statut: string;
  actionAdmin: string | null;
  adminNom: string | null;
  dateSignalement: string;
  dateTraitement: string | null;
  messageContenu: string | null;
  messageMasque: boolean;
}

export interface AuditEntry {
  id: string;
  conversationId: string | null;
  action: string;
  acteurNom: string | null;
  acteurRole: string | null;
  details: string | null;
  dateAction: string;
}

export interface ConfirmDialogData {
  type: 'conversation' | 'message' | 'quitter' | 'retirer_participant';
  id: string;
  participantId?: number;
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

export interface FichierUploadResult {
  url: string;
  nom: string;
  taille: number;
}

export type SupervisionScope = 'TOUTES' | 'GROUPES' | 'SIGNALEES' | 'ARCHIVEES';

export interface MessagerieAdminStats {
  conversations: number;
  groupes: number;
  messagesAujourdhui: number;
  signalementsATraiter: number;
  nouvellesConversationsSemaine: number;
  nouveauxGroupesSemaine: number;
  heurePicActivite: number | null;
  heurePicLibelle: string | null;
}

export interface AdminConversationSummary {
  id: string;
  conversation: string;
  type: 'groupe' | 'directe' | string;
  membres: number;
  messages: number;
  derniereActivite: string | null;
  statut: 'active' | 'signalee' | 'archivee' | string;
  archivee: boolean;
  signalee: boolean;
  dernierMessageContenu: string | null;
  dernierMessageSenderNom: string | null;
  nonLusCount: number;
  participants: ParticipantSummary[];
  signalementsCount?: number;
}

export interface MessagerieAdminDashboard {
  stats: MessagerieAdminStats;
  conversations: AdminConversationSummary[];
  totalElements?: number;
  totalPages?: number;
  page?: number;
  size?: number;
}

export interface AdminConversationDetail {
  conversation: AdminConversationSummary;
  dateCreation: string | null;
  dateArchivage: string | null;
  dateSignalement: string | null;
  createurId?: number | null;
  createurNom?: string | null;
  totalMessages?: number;
  nonLusCount?: number;
  messages: Message[];
  signalements?: SignalementItem[];
  auditLog?: AuditEntry[];
}

export interface PucePalette {
  bg: string;
  text: string;
}

export const COULEURS_PUCES: readonly PucePalette[] = [
  { bg: '#E1F5EE', text: '#0F6E56' }, // vert
  { bg: '#EEEDFE', text: '#534AB7' }, // violet
  { bg: '#FAECE7', text: '#993C1D' }, // corail
  { bg: '#E6F1FB', text: '#185FA5' }, // bleu
  { bg: '#FBEAF0', text: '#993556' }  // rose
];

export function couleurPourUtilisateur(id: number): PucePalette {
  const index = Math.abs(Math.trunc(id || 0)) % COULEURS_PUCES.length;
  return COULEURS_PUCES[index];
}

export function normaliserTexte(valeur: string | null | undefined): string {
  return (valeur ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

export function extrairePrenom(nomComplet: string | null | undefined): string {
  const parts = (nomComplet ?? '').trim().split(/\s+/).filter(Boolean);
  return parts[0] || 'Membre';
}

export function formaterNomCourt(nomComplet: string | null | undefined): string {
  const parts = (nomComplet ?? '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'Membre';
  if (parts.length === 1) return parts[0];
  return `${parts[0]} ${parts[1].charAt(0).toUpperCase()}.`;
}

export function formaterInitiales(nom: string | null | undefined): string {
  const parts = (nom ?? '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'SM';
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
}

export function formaterRoleLibelle(role: string | null | undefined): string {
  if (!role) return 'Membre';
  switch (role.toUpperCase()) {
    case 'CANDIDAT':
      return 'Candidat';
    case 'RECRUTEUR':
      return 'Recruteur';
    case 'ADMIN':
      return 'Administrateur';
    default:
      return role;
  }
}
