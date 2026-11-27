import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { messagerieConfig } from './messagerie.config';
import {
  AdminConversationDetail,
  AdminConversationSummary,
  Conversation,
  ConversationType,
  FichierUploadResult,
  Message,
  MessageType,
  MessagerieAdminDashboard,
  SignalementActionAdmin,
  SignalementItem,
  SignalementMotif,
  SupervisionScope,
  UserSummary
} from './messagerie.models';

@Injectable({ providedIn: 'root' })
export class MessagerieService {
  private readonly http = inject(HttpClient);
  // Annuaire, auth et utilisateurs : passerelle (environment.apiUrl, port 8080).
  // Conversations et messages : messagerie-service (messagerie.config.ts, port 8086).
  private readonly apiUrl = environment.apiUrl;
  private readonly messagerieOrigin = messagerieConfig.apiUrl;
  private readonly messagerieApi = `${this.messagerieOrigin}/skillmatch/messagerie`;

  /** Liste les conversations de l'utilisateur connecté */
  getConversations(): Observable<Conversation[]> {
    return this.http
      .get<Conversation[]>(`${this.messagerieApi}/conversations`)
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeConversation(item))));
  }

  /** Récupère le détail d'une conversation */
  getConversation(id: string): Observable<Conversation> {
    return this.http
      .get<Conversation>(`${this.messagerieApi}/conversations/${id}`)
      .pipe(map((item) => this.normalizeConversation(item)));
  }

  /** Récupère l'historique des messages d'une conversation */
  getMessages(conversationId: string): Observable<Message[]> {
    return this.http
      .get<Message[]>(`${this.messagerieApi}/conversations/${conversationId}/messages`)
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeMessage(item))));
  }

  /** Crée une nouvelle conversation directe ou un groupe */
  createConversation(
    participantIds: number[],
    nomGroupe?: string | null,
    type: ConversationType = 'direct'
  ): Observable<Conversation> {
    return this.http
      .post<Conversation>(`${this.messagerieApi}/conversations`, {
        participantIds,
        nomGroupe: type === 'groupe' ? nomGroupe : null,
        type: type === 'groupe' ? 'GROUPE' : 'DIRECT'
      })
      .pipe(map((item) => this.normalizeConversation(item)));
  }

  /** Modifie la liste des participants d'une conversation */
  updateConversation(
    id: string,
    participantIds: number[],
    nomGroupe?: string | null,
    type?: ConversationType
  ): Observable<Conversation> {
    const resolvedType: ConversationType = type ?? (participantIds.length >= 3 ? 'groupe' : 'direct');
    return this.http
      .put<Conversation>(`${this.messagerieApi}/conversations/${id}`, {
        participantIds,
        nomGroupe: resolvedType === 'groupe' ? nomGroupe : null,
        type: resolvedType === 'groupe' ? 'GROUPE' : 'DIRECT'
      })
      .pipe(map((item) => this.normalizeConversation(item)));
  }

  /** Quitte un groupe dont l'utilisateur est membre */
  quitterGroupe(id: string): Observable<Conversation> {
    return this.http
      .post<Conversation>(`${this.messagerieApi}/conversations/${id}/quitter`, {})
      .pipe(map((item) => this.normalizeConversation(item)));
  }

  /** Supprime une conversation et tous ses messages */
  deleteConversation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.messagerieApi}/conversations/${id}`);
  }

  /** Envoie un nouveau message dans une conversation */
  sendMessage(
    conversationId: string,
    contenu: string,
    options?: {
      type?: MessageType;
      fichierNom?: string;
      fichierUrl?: string;
      fichierTaille?: number;
    }
  ): Observable<Message> {
    return this.http
      .post<Message>(`${this.messagerieApi}/conversations/${conversationId}/messages`, {
        contenu,
        type: options?.type ?? 'texte',
        fichierNom: options?.fichierNom ?? null,
        fichierUrl: options?.fichierUrl ?? null,
        fichierTaille: options?.fichierTaille ?? null
      })
      .pipe(map((item) => this.normalizeMessage(item)));
  }

  /** Modifie le contenu d'un message existant */
  updateMessage(id: string, contenu: string): Observable<Message> {
    return this.http
      .put<Message>(`${this.messagerieApi}/messages/${id}`, { contenu })
      .pipe(map((item) => this.normalizeMessage(item)));
  }

  /** Supprime un message */
  deleteMessage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.messagerieApi}/messages/${id}`);
  }

  /** Marque un message comme lu */
  markAsRead(id: string): Observable<Message> {
    return this.http
      .patch<Message>(`${this.messagerieApi}/messages/${id}/lu`, {})
      .pipe(map((item) => this.normalizeMessage(item)));
  }

  /** Recherche les utilisateurs par nom via messagerie-service (qui s'appuie sur UserClient). */
  searchDirectory(query: string, role: string | null = null): Observable<UserSummary[]> {
    const params: Record<string, string> = { q: query };
    if (role) params['role'] = role;
    return this.http.get<UserSummary[]>(`${this.messagerieApi}/utilisateurs/recherche`, { params });
  }

  /** Signale un message avec un motif et un commentaire facultatif. */
  signalerMessage(
    messageId: string,
    motif: SignalementMotif,
    commentaire?: string
  ): Observable<SignalementItem> {
    return this.http.post<SignalementItem>(`${this.messagerieApi}/messages/${messageId}/signalements`, {
      motif,
      commentaire: commentaire?.trim() ? commentaire.trim() : null
    });
  }

  /** Résout un utilisateur via l'annuaire de la Gateway. */
  lookupUser(id: number): Observable<UserSummary> {
    return this.http.get<UserSummary>(`${this.apiUrl}/skillmatch/annuaire/utilisateurs/${id}`);
  }

  /** Joindre un fichier à une conversation (pdf, jpg, png, doc, docx ; 10 Mo max). */
  uploadFile(conversationId: string, file: File): Observable<FichierUploadResult> {
    const formData = new FormData();
    formData.append('fichier', file);
    return this.http.post<FichierUploadResult>(
      `${this.messagerieApi}/conversations/${conversationId}/fichiers`,
      formData
    );
  }

  /** Charge un fichier protégé par JWT en Blob (compatible balise img et téléchargement). */
  loadFileBlob(fileUrl: string): Observable<Blob> {
    const absoluteUrl = fileUrl.startsWith('http://') || fileUrl.startsWith('https://')
      ? fileUrl
      : `${this.messagerieOrigin}${fileUrl.startsWith('/') ? '' : '/'}${fileUrl}`;
    return this.http.get(absoluteUrl, { responseType: 'blob' });
  }

  /** Supervision admin : KPI + liste filtrable et paginée des conversations. */
  getAdminSupervision(
    scope: SupervisionScope = 'TOUTES',
    query = '',
    page?: number,
    size?: number
  ): Observable<MessagerieAdminDashboard> {
    const params: Record<string, string> = {};
    if (scope) params['scope'] = scope;
    if (query.trim()) params['query'] = query.trim();
    if (page !== undefined) params['page'] = String(page);
    if (size !== undefined) params['size'] = String(size);
    return this.http.get<MessagerieAdminDashboard>(`${this.messagerieApi}/admin/supervision`, { params });
  }

  /** Supervision admin : export CSV des conversations filtrées. */
  exportAdminSupervisionCsv(scope: SupervisionScope = 'TOUTES', query = ''): Observable<Blob> {
    const params: Record<string, string> = {};
    if (scope) params['scope'] = scope;
    if (query.trim()) params['query'] = query.trim();
    return this.http.get(`${this.messagerieApi}/admin/supervision/export`, {
      params,
      responseType: 'blob'
    });
  }

  /** Supervision admin : export CSV d'une conversation. */
  exportAdminConversationCsv(id: string): Observable<Blob> {
    return this.http.get(`${this.messagerieApi}/admin/conversations/${id}/export`, {
      responseType: 'blob'
    });
  }

  /** Supervision admin : détail d'une conversation et journal d'audit. */
  getAdminConversationDetail(id: string): Observable<AdminConversationDetail> {
    return this.http.get<AdminConversationDetail>(`${this.messagerieApi}/admin/conversations/${id}`);
  }

  /** Supervision admin : mise à jour du statut de modération (archiver/désarchiver). */
  updateAdminConversationModeration(
    id: string,
    payload: { signalee?: boolean; archivee?: boolean }
  ): Observable<AdminConversationSummary> {
    return this.http.patch<AdminConversationSummary>(
      `${this.messagerieApi}/admin/conversations/${id}/moderation`,
      payload
    );
  }

  /** Supervision admin : consulter le contenu d'un message signalé (journalisé en audit). */
  consulterSignalement(signalementId: string): Observable<SignalementItem> {
    return this.http.post<SignalementItem>(
      `${this.messagerieApi}/admin/signalements/${signalementId}/consulter`,
      {}
    );
  }

  /** Supervision admin : traiter un signalement (Ignorer, Avertir, Masquer, Suspendre). */
  traiterSignalement(
    signalementId: string,
    action: SignalementActionAdmin,
    noteAdmin?: string
  ): Observable<SignalementItem> {
    return this.http.patch<SignalementItem>(
      `${this.messagerieApi}/admin/signalements/${signalementId}/traiter`,
      { action, noteAdmin: noteAdmin?.trim() || null }
    );
  }

  private normalizeConversation(c: Conversation): Conversation {
    const isGroup = Boolean(c.estGroupe) || String(c.type ?? '').toLowerCase() === 'groupe';
    return {
      ...c,
      type: isGroup ? 'groupe' : 'direct',
      estGroupe: isGroup,
      participants: c.participants ?? []
    };
  }

  private normalizeMessage(m: Message): Message {
    const rawType = String(m.type ?? 'texte').toLowerCase();
    const type: MessageType =
      rawType === 'systeme' || rawType === 'système'
        ? 'systeme'
        : rawType === 'fichier'
          ? 'fichier'
          : 'texte';
    return {
      ...m,
      type
    };
  }
}
