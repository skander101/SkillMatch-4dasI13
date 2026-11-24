import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  OnDestroy,
  Output,
  ViewChild,
  computed,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AuthService } from '../user/auth.service';
import {
  ConfirmDialogData,
  Conversation,
  ConversationFilterKey,
  Message,
  ParticipantSummary,
  PucePalette,
  UserSummary,
  couleurPourUtilisateur,
  extrairePrenom,
  formaterInitiales,
  formaterRoleLibelle,
  normaliserTexte
} from './messagerie.models';
import { MessagerieService } from './messagerie.service';
import { PersonSelectorComponent } from './person-selector/person-selector.component';
import { SupervisionComponent } from './supervision/supervision.component';

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_UPLOAD_EXTENSIONS = new Set(['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx']);

@Component({
  selector: 'app-messagerie',
  standalone: true,
  imports: [CommonModule, FormsModule, PersonSelectorComponent],
  templateUrl: './messagerie.html',
  styleUrl: './messagerie.css'
})
export class MessagerieComponent implements OnDestroy {
  @Output() close = new EventEmitter<void>();
  @ViewChild('thread') private thread?: ElementRef<HTMLElement>;
  @ViewChild('groupModalEl') private groupModalEl?: ElementRef<HTMLElement>;
  @ViewChild('newModalEl') private newModalEl?: ElementRef<HTMLElement>;
  @ViewChild('confirmModalEl') private confirmModalEl?: ElementRef<HTMLElement>;
  @ViewChild('newConvSelector') private newConvSelector?: PersonSelectorComponent;

  private readonly api = inject(MessagerieService);
  private readonly auth = inject(AuthService);
  private readonly sanitizer = inject(DomSanitizer);

  private pollTimer?: number;
  private successTimer?: number;
  private typingTimer?: number;
  private modalCloseTimer?: number;
  private lastFocusedElement: HTMLElement | null = null;

  private readonly directoryCache = new Map<number, UserSummary>();
  private readonly directoryPending = new Set<number>();
  private readonly rawBlobUrls = new Map<string, string>();
  private readonly loadingBlobs = new Set<string>();

  readonly resolvedUsers = signal<Record<number, UserSummary>>({});
  readonly filePreviewUrls = signal<Record<string, SafeUrl>>({});
  readonly groupPhotos = signal<Record<string, string>>({});

  readonly currentUser = this.auth.currentUser;
  readonly isAdmin = this.auth.isAdmin;
  readonly adminView = signal<'supervision' | 'discussions'>('supervision');
  readonly conversations = signal<Conversation[]>([]);
  readonly messages = signal<Message[]>([]);
  readonly selected = signal<Conversation | null>(null);

  readonly loading = signal(false);
  readonly messagesLoading = signal(false);
  readonly sending = signal(false);
  readonly creating = signal(false);
  readonly savingEdit = signal(false);
  readonly deleting = signal(false);

  readonly error = signal('');
  readonly success = signal('');

  // === Menus & défilement ===
  readonly menuOpen = signal(false);
  readonly sortMenuOpen = signal(false);
  readonly showScrollBottom = signal(false);

  // === Nouvelle conversation directe ===
  readonly showNewModal = signal(false);
  readonly newModalClosing = signal(false);
  readonly recipientSelection = signal<UserSummary[]>([]);
  readonly directoryRole = signal<'ALL' | 'CANDIDAT' | 'RECRUTEUR'>('ALL');
  readonly recipientLookupError = signal('');

  // === Créer un groupe ===
  readonly showGroupModal = signal(false);
  readonly groupModalClosing = signal(false);
  readonly groupNom = signal('');
  readonly groupNomTouched = signal(false);
  readonly groupPhotoPreview = signal<string | null>(null);
  readonly groupParticipants = signal<UserSummary[]>([]);
  readonly groupCreating = signal(false);
  readonly groupError = signal('');

  // === Panneau Gérer les participants ===
  readonly showParticipantsPanel = signal(false);
  readonly panelParticipants = signal<UserSummary[]>([]);
  readonly panelNomGroupe = signal('');
  readonly panelAddingToDirect = signal(false);
  readonly panelSaving = signal(false);
  readonly panelError = signal('');
  readonly panelPendingRemoval = signal<{ user: UserSummary; nextList: UserSummary[] } | null>(null);

  // === Discussion & rédaction ===
  readonly draft = signal('');
  readonly editingMessageId = signal<string | null>(null);
  readonly editingContent = signal('');
  readonly search = signal('');
  readonly filter = signal<ConversationFilterKey>('all');
  readonly sortOrder = signal<'newest' | 'oldest'>('newest');
  readonly mobileThread = signal(false);
  readonly typingUser = signal<string | null>(null);

  // === Dialogue de confirmation ===
  readonly confirmDeleteDialog = signal<ConfirmDialogData | null>(null);
  readonly showConvertDirectPrompt = signal(false);

  // Validation dynamique nom de groupe
  readonly groupNameError = computed(() => {
    if (!this.groupNomTouched()) return '';
    const nom = this.groupNom().trim();
    if (!nom) return 'Le nom du groupe est obligatoire.';
    if (nom.length < 3 || nom.length > 50) {
      return 'Le nom du groupe doit contenir entre 3 et 50 caractères.';
    }
    return '';
  });

  // Contacts récents dérivés des conversations existantes (sans l'utilisateur connecté ni les admins pour les non-admins)
  readonly contactsRecents = computed<UserSummary[]>(() => {
    const myId = this.currentUser()?.id;
    const isAdminUser = this.isAdmin();
    const usersMap = this.resolvedUsers();
    const seen = new Set<number>();
    const list: UserSummary[] = [];

    const orderedConvs = [...this.conversations()].sort((a, b) => {
      const aTime = a.dernierMessageAt ? new Date(a.dernierMessageAt).getTime() : 0;
      const bTime = b.dernierMessageAt ? new Date(b.dernierMessageAt).getTime() : 0;
      return bTime - aTime;
    });

    for (const conv of orderedConvs) {
      for (const p of conv.participants ?? []) {
        if (!p.id || p.id === myId || seen.has(p.id)) continue;
        const resolved = usersMap[p.id];
        const nom = p.nom && p.nom !== 'Membre' ? p.nom : resolved?.nom;
        const role = p.role ?? resolved?.role ?? null;
        if (!nom) continue;
        if (!isAdminUser && String(role ?? '').toUpperCase() === 'ADMIN') continue;
        seen.add(p.id);
        list.push({ id: p.id, nom, role });
      }
    }
    return list;
  });

  readonly directConversationUserIds = computed<number[]>(() => {
    const myId = this.currentUser()?.id;
    if (!myId) return [];
    const ids: number[] = [];
    for (const c of this.conversations()) {
      if (!this.isGroupConv(c) && c.participantIds.length === 2) {
        const other = c.participantIds.find((id) => id !== myId);
        if (other) ids.push(other);
      }
    }
    return ids;
  });

  readonly filteredConversations = computed(() => {
    const term = normaliserTexte(this.search());
    const mode = this.filter();
    const usersMap = this.resolvedUsers();

    const filtered = this.conversations().filter((conversation) => {
      if (mode === 'unread' && !(conversation.nonLusCount && conversation.nonLusCount > 0)) {
        return false;
      }
      if (!term) return true;

      const resolvedName = normaliserTexte(this.displayName(conversation));
      const participantTexts = (conversation.participants ?? [])
        .map((p) => {
          const fallback = usersMap[p.id];
          return `${p.nom} ${fallback?.nom ?? ''} ${p.role ?? fallback?.role ?? ''}`;
        })
        .join(' ');
      const preview = normaliserTexte(conversation.dernierMessageContenu);

      return (
        resolvedName.includes(term) ||
        normaliserTexte(participantTexts).includes(term) ||
        preview.includes(term)
      );
    });

    return filtered.sort((a, b) => {
      const aTime = a.dernierMessageAt ? new Date(a.dernierMessageAt).getTime() : 0;
      const bTime = b.dernierMessageAt ? new Date(b.dernierMessageAt).getTime() : 0;
      return this.sortOrder() === 'newest' ? bTime - aTime : aTime - bTime;
    });
  });

  readonly counts = computed(() => {
    const all = this.conversations().length;
    const unread = this.conversations().filter((c) => (c.nonLusCount ?? 0) > 0).length;
    return { all, unread };
  });

  constructor() {
    this.loadConversations();
    this.pollTimer = window.setInterval(() => this.refreshQuietly(), 6000);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.pollTimer);
    window.clearTimeout(this.successTimer);
    window.clearTimeout(this.typingTimer);
    window.clearTimeout(this.modalCloseTimer);
    for (const objectUrl of this.rawBlobUrls.values()) {
      URL.revokeObjectURL(objectUrl);
    }
    this.rawBlobUrls.clear();
  }

  @HostListener('document:click')
  onGlobalClick(): void {
    this.menuOpen.set(false);
    this.sortMenuOpen.set(false);
  }

  @HostListener('window:keydown.escape', ['$event'])
  onGlobalEscape(event: Event): void {
    if (this.confirmDeleteDialog()) {
      event.preventDefault();
      this.cancelDelete();
      return;
    }
    if (this.showGroupModal()) {
      event.preventDefault();
      this.closeGroupModal();
      return;
    }
    if (this.showNewModal()) {
      event.preventDefault();
      this.closeNewModal();
      return;
    }
    if (this.menuOpen() || this.sortMenuOpen()) {
      this.menuOpen.set(false);
      this.sortMenuOpen.set(false);
    }
  }

  // ── Chargement & liste ──
  loadConversations(): void {
    this.loading.set(true);
    this.error.set('');

    this.api.getConversations().subscribe({
      next: (data) => {
        this.conversations.set(data);
        this.syncSelected(data);
        this.enrichMissingParticipants(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.formatErrorMessage(err, 'Impossible de charger les conversations.'));
      }
    });
  }

  selectConversation(conversation: Conversation): void {
    this.selected.set(conversation);
    this.mobileThread.set(true);
    this.messages.set([]);
    this.editingMessageId.set(null);
    this.menuOpen.set(false);
    this.showConvertDirectPrompt.set(false);
    this.showScrollBottom.set(false);
    this.error.set('');
    this.showParticipantsPanel.set(false);
    this.loadMessages(conversation.id, true);
  }

  showList(): void {
    this.mobileThread.set(false);
  }

  toggleSortMenu(event: Event): void {
    event.stopPropagation();
    this.sortMenuOpen.update((v) => !v);
    this.menuOpen.set(false);
  }

  setSortOrder(order: 'newest' | 'oldest'): void {
    this.sortOrder.set(order);
    this.sortMenuOpen.set(false);
  }

  toggleActionMenu(event: Event): void {
    event.stopPropagation();
    this.menuOpen.update((v) => !v);
    this.sortMenuOpen.set(false);
  }

  // ── Nouvelle conversation directe ──
  openNewModal(): void {
    this.rememberFocus();
    this.newModalClosing.set(false);
    this.showNewModal.set(true);
    this.recipientSelection.set([]);
    this.recipientLookupError.set('');
    this.directoryRole.set('ALL');
    window.setTimeout(() => this.focusFirstInModal(this.newModalEl?.nativeElement), 20);
  }

  closeNewModal(): void {
    if (this.creating() || this.newModalClosing()) return;
    this.newModalClosing.set(true);
    window.clearTimeout(this.modalCloseTimer);
    this.modalCloseTimer = window.setTimeout(() => {
      this.showNewModal.set(false);
      this.newModalClosing.set(false);
      this.recipientSelection.set([]);
      this.recipientLookupError.set('');
      this.restoreFocus();
    }, 150);
  }

  setDirectoryRole(role: 'ALL' | 'CANDIDAT' | 'RECRUTEUR'): void {
    this.directoryRole.set(role);
    window.setTimeout(() => this.newConvSelector?.onRoleFilterReload(), 0);
  }

  onNewPersonSelectorChange(users: UserSummary[]): void {
    this.recipientSelection.set(users);
    this.recipientLookupError.set('');
  }

  recipientLookup(): UserSummary | null {
    return this.recipientSelection()[0] ?? null;
  }

  createConversation(): void {
    const selectedRecipient = this.recipientLookup();
    const participantId = selectedRecipient?.id ?? 0;
    const currentId = this.currentUser()?.id;

    if (!currentId) {
      this.recipientLookupError.set('Utilisateur non connecté. Veuillez vous reconnecter.');
      return;
    }

    if (!selectedRecipient || participantId <= 0) {
      this.recipientLookupError.set('Sélectionnez un destinataire dans la liste.');
      return;
    }

    if (participantId === currentId) {
      this.recipientLookupError.set('Vous ne pouvez pas créer une conversation avec vous-même.');
      return;
    }

    if (!this.isAdmin() && String(selectedRecipient.role ?? '').toUpperCase() === 'ADMIN') {
      this.recipientLookupError.set('Vous ne pouvez pas écrire à un administrateur.');
      return;
    }

    const existing = this.findExistingConversation(currentId, participantId);
    if (existing) {
      this.showNewModal.set(false);
      this.recipientSelection.set([]);
      this.flash('Conversation existante ouverte');
      this.selectConversation(existing);
      return;
    }

    this.creating.set(true);
    this.error.set('');

    this.api.createConversation([currentId, participantId], null, 'direct').subscribe({
      next: (conversation) => {
        this.creating.set(false);
        this.showNewModal.set(false);
        this.recipientSelection.set([]);
        this.conversations.update((items) => [
          conversation,
          ...items.filter((item) => item.id !== conversation.id)
        ]);
        this.enrichMissingParticipants([conversation]);
        this.flash('Conversation créée');
        this.selectConversation(conversation);
      },
      error: (err) => {
        this.creating.set(false);
        if (err?.status === 409) {
          this.openExistingConversation(currentId, participantId);
          return;
        }
        if (err?.status === 403) {
          this.recipientLookupError.set('Vous ne pouvez pas écrire à un administrateur.');
          return;
        }
        const msg = this.formatErrorMessage(err, 'Création de la conversation impossible.');
        this.recipientLookupError.set(msg);
      }
    });
  }

  // ── Créer un groupe ──
  openGroupModal(): void {
    this.rememberFocus();
    this.groupModalClosing.set(false);
    this.showGroupModal.set(true);
    this.groupNom.set('');
    this.groupNomTouched.set(false);
    this.groupPhotoPreview.set(null);
    this.groupError.set('');
    const moi = this.currentUser();
    this.groupParticipants.set(
      moi ? [{ id: moi.id, nom: moi.nom, role: moi.role }] : []
    );
    window.setTimeout(() => this.focusFirstInModal(this.groupModalEl?.nativeElement), 20);
  }

  closeGroupModal(): void {
    if (this.groupCreating() || this.groupModalClosing()) return;
    this.groupModalClosing.set(true);
    window.clearTimeout(this.modalCloseTimer);
    this.modalCloseTimer = window.setTimeout(() => {
      this.showGroupModal.set(false);
      this.groupModalClosing.set(false);
      this.groupNom.set('');
      this.groupNomTouched.set(false);
      this.groupPhotoPreview.set(null);
      this.groupParticipants.set([]);
      this.groupError.set('');
      this.restoreFocus();
    }, 150);
  }

  onGroupNameChange(value: string): void {
    this.groupNom.set(value);
    if (!this.groupNomTouched() && value.length > 0) {
      this.groupNomTouched.set(true);
    }
    this.groupError.set('');
  }

  onGroupPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.groupError.set('Veuillez choisir une image valide (PNG, JPG).');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.groupError.set('La photo du groupe ne doit pas dépasser 5 Mo.');
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.groupPhotoPreview.set(reader.result);
      }
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  onGroupPersonSelectorChange(users: UserSummary[]): void {
    this.groupParticipants.set(users);
    this.groupError.set('');
  }

  createGroup(): void {
    this.groupNomTouched.set(true);
    const nom = this.groupNom().trim();
    const participants = this.groupParticipants();
    const currentId = this.currentUser()?.id;

    if (!currentId) {
      this.groupError.set('Utilisateur non connecté.');
      return;
    }
    if (!nom || nom.length < 3 || nom.length > 50) {
      this.groupError.set('Le nom du groupe doit contenir entre 3 et 50 caractères.');
      return;
    }
    const autresCount = participants.filter((p) => p.id !== currentId).length;
    if (autresCount < 2) {
      this.groupError.set('Ajoutez au moins 2 personnes en plus de vous.');
      return;
    }
    if (!this.isAdmin() && participants.some((p) => String(p.role ?? '').toUpperCase() === 'ADMIN')) {
      this.groupError.set('Vous ne pouvez pas écrire à un administrateur.');
      return;
    }

    this.groupCreating.set(true);
    this.groupError.set('');

    const ids = participants.map((p) => p.id);
    const photo = this.groupPhotoPreview();

    this.api.createConversation(ids, nom, 'groupe').subscribe({
      next: (conversation) => {
        this.groupCreating.set(false);
        if (photo) {
          this.groupPhotos.update((map) => ({ ...map, [conversation.id]: photo }));
        }
        this.conversations.update((items) => [
          conversation,
          ...items.filter((item) => item.id !== conversation.id)
        ]);
        this.enrichMissingParticipants([conversation]);
        this.selectConversation(conversation);
        this.flash('Groupe créé');

        this.groupModalClosing.set(true);
        window.clearTimeout(this.modalCloseTimer);
        this.modalCloseTimer = window.setTimeout(() => {
          this.showGroupModal.set(false);
          this.groupModalClosing.set(false);
          this.groupNom.set('');
          this.groupNomTouched.set(false);
          this.groupPhotoPreview.set(null);
          this.groupParticipants.set([]);
          this.restoreFocus();
        }, 150);
      },
      error: (err) => {
        this.groupCreating.set(false);
        if (err?.status === 403) {
          this.groupError.set('Vous ne pouvez pas écrire à un administrateur.');
          return;
        }
        this.groupError.set(this.formatErrorMessage(err, 'Impossible de créer le groupe.'));
      }
    });
  }

  // ── Panneau Gérer les participants ──
  openParticipantsPanel(conversation: Conversation): void {
    if (this.showParticipantsPanel()) {
      this.closeParticipantsPanel();
      return;
    }
    this.menuOpen.set(false);
    this.panelError.set('');
    this.panelSaving.set(false);
    this.panelAddingToDirect.set(false);
    this.panelPendingRemoval.set(null);

    const participantsList = this.buildParticipantUsers(conversation);
    this.panelParticipants.set(participantsList);
    this.panelNomGroupe.set(conversation.nomGroupe ?? '');
    this.showParticipantsPanel.set(true);

    this.api.getConversation(conversation.id).subscribe({
      next: (fresh) => {
        this.syncSelected([fresh]);
        this.panelParticipants.set(this.buildParticipantUsers(fresh));
        this.panelNomGroupe.set(fresh.nomGroupe ?? '');
      },
      error: () => {}
    });
  }

  closeParticipantsPanel(): void {
    this.showParticipantsPanel.set(false);
    this.panelAddingToDirect.set(false);
    this.panelNomGroupe.set('');
    this.panelError.set('');
    this.panelPendingRemoval.set(null);
  }

  onPanelPersonSelectorChange(nextUsers: UserSummary[]): void {
    const conv = this.selected();
    if (!conv) return;
    const current = this.panelParticipants();
    this.panelError.set('');

    if (nextUsers.length < current.length) {
      const removed = current.find((p) => !nextUsers.some((n) => n.id === p.id));
      if (!removed) return;
      if (removed.id === conv.candidatUserId) {
        this.panelError.set('Vous restez toujours dans le groupe.');
        return;
      }
      this.rememberFocus();
      this.panelPendingRemoval.set({ user: removed, nextList: nextUsers });
      this.confirmDeleteDialog.set({
        type: 'retirer_participant',
        id: conv.id,
        participantId: removed.id,
        title: 'Retirer du groupe ?',
        message: `Retirer ${extrairePrenom(removed.nom)} du groupe ?`,
        confirmLabel: 'Retirer',
        danger: true
      });
      return;
    }

    if (nextUsers.length > current.length) {
      if (!this.isAdmin() && nextUsers.some((p) => String(p.role ?? '').toUpperCase() === 'ADMIN')) {
        this.panelError.set('Vous ne pouvez pas écrire à un administrateur.');
        return;
      }
      this.panelParticipants.set(nextUsers);

      if (!this.isGroupConv(conv) && nextUsers.length >= 3) {
        this.panelAddingToDirect.set(true);
        if (!this.panelNomGroupe().trim()) {
          const other = nextUsers.find((p) => p.id !== this.currentUser()?.id);
          const defaultName = other ? `Groupe avec ${extrairePrenom(other.nom)}` : 'Nouveau groupe';
          this.panelNomGroupe.set(defaultName.substring(0, 50));
        }
      } else {
        this.sauvegarderParticipants(nextUsers, this.isGroupConv(conv) ? (conv.nomGroupe ?? null) : null, this.isGroupConv(conv) ? 'groupe' : 'direct');
      }
    }
  }

  validerPassageDirectVersGroupe(): void {
    const conv = this.selected();
    if (!conv) return;
    const nom = this.panelNomGroupe().trim();
    if (nom.length < 3 || nom.length > 50) {
      this.panelError.set('Le nom du groupe doit contenir entre 3 et 50 caractères.');
      return;
    }
    this.sauvegarderParticipants(this.panelParticipants(), nom, 'groupe');
  }

  annulerPassageDirectVersGroupe(): void {
    const conv = this.selected();
    if (!conv) return;
    this.panelAddingToDirect.set(false);
    this.panelNomGroupe.set(conv.nomGroupe ?? '');
    this.panelParticipants.set(this.buildParticipantUsers(conv));
    this.panelError.set('');
  }

  convertirEnConversationDirecte(): void {
    const conv = this.selected();
    if (!conv || !this.isGroupConv(conv) || conv.participantIds.length !== 2) return;
    this.sauvegarderParticipants(this.buildParticipantUsers(conv), null, 'direct');
  }

  private sauvegarderParticipants(
    users: UserSummary[],
    nomGroupe: string | null,
    targetType: 'direct' | 'groupe'
  ): void {
    const conv = this.selected();
    if (!conv) return;
    const ids = users.map((u) => u.id);
    if (ids.length < 2) {
      this.panelError.set('Une conversation doit contenir au moins 2 participants.');
      return;
    }

    this.panelSaving.set(true);
    this.panelError.set('');

    this.api.updateConversation(conv.id, ids, nomGroupe, targetType).subscribe({
      next: (updated) => {
        this.panelSaving.set(false);
        this.panelAddingToDirect.set(false);
        this.showConvertDirectPrompt.set(false);
        this.conversations.update((items) =>
          items.map((c) => (c.id === updated.id ? updated : c))
        );
        this.selected.set(updated);
        this.enrichMissingParticipants([updated]);
        this.panelParticipants.set(this.buildParticipantUsers(updated));
        this.panelNomGroupe.set(updated.nomGroupe ?? '');
        this.loadMessages(updated.id, false);
        this.flash(targetType === 'direct' ? 'Converti en conversation directe' : 'Participants mis à jour');
      },
      error: (err) => {
        this.panelSaving.set(false);
        this.panelParticipants.set(this.buildParticipantUsers(conv));
        if (err?.status === 403) {
          this.panelError.set('Vous ne pouvez pas écrire à un administrateur.');
          return;
        }
        this.panelError.set(this.formatErrorMessage(err, 'Impossible de modifier les participants.'));
      }
    });
  }

  // ── Menu ⋯ : Quitter le groupe & Supprimer ──
  requestQuitterGroupe(conversation: Conversation): void {
    this.menuOpen.set(false);
    if (!this.isGroupConv(conversation)) return;

    if (conversation.participantIds.length <= 2) {
      this.showConvertDirectPrompt.set(true);
      this.showParticipantsPanel.set(true);
      this.panelParticipants.set(this.buildParticipantUsers(conversation));
      return;
    }

    this.rememberFocus();
    this.confirmDeleteDialog.set({
      type: 'quitter',
      id: conversation.id,
      title: 'Quitter ce groupe ?',
      message: `Voulez-vous vraiment quitter « ${this.displayName(conversation)} » ?`,
      confirmLabel: 'Quitter',
      danger: true
    });
  }

  requestDeleteConversation(conversation: Conversation): void {
    this.menuOpen.set(false);
    if (!this.canSupprimer(conversation)) {
      this.error.set('Seul le créateur ou un administrateur peut supprimer cette conversation.');
      return;
    }
    this.rememberFocus();
    this.confirmDeleteDialog.set({
      type: 'conversation',
      id: conversation.id,
      title: 'Supprimer cette conversation ?',
      message: `Voulez-vous vraiment supprimer la conversation « ${this.displayName(conversation)} » et tous ses messages ?`,
      confirmLabel: 'Supprimer',
      danger: true
    });
  }

  // ── Messages ──
  sendMessage(): void {
    const conversation = this.selected();
    const contenu = this.draft().trim();

    if (!conversation || !contenu || this.sending()) return;

    this.sending.set(true);
    this.error.set('');

    this.api.sendMessage(conversation.id, contenu).subscribe({
      next: (message) => {
        this.messages.update((items) => [...items, message]);
        this.draft.set('');
        this.sending.set(false);
        this.typingUser.set(null);

        this.conversations.update((list) =>
          list.map((c) =>
            c.id === conversation.id
              ? {
                  ...c,
                  dernierMessageContenu: message.contenu,
                  dernierMessageAt: message.date,
                  dernierMessageSenderId: message.senderId,
                  dernierMessageSenderNom: extrairePrenom(this.currentUser()?.nom)
                }
              : c
          )
        );

        this.scrollThread();
      },
      error: (err) => {
        this.sending.set(false);
        if (err?.status === 403) {
          this.error.set('Vous ne pouvez pas écrire à un administrateur.');
          return;
        }
        this.error.set(this.formatErrorMessage(err, "Échec de l'envoi du message."));
      }
    });
  }

  ajouterFichier(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!ALLOWED_UPLOAD_EXTENSIONS.has(ext)) {
      this.error.set('Format non supporté (PDF, JPG, PNG, DOC, DOCX uniquement).');
      input.value = '';
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      this.error.set('Le fichier est trop lourd (10 Mo maximum).');
      input.value = '';
      return;
    }
    const conversation = this.selected();
    if (!conversation) return;

    this.sending.set(true);
    this.error.set('');

    this.api.uploadFile(conversation.id, file).subscribe({
      next: (result) => {
        const contenu = `[Fichier] ${result.nom}`;
        this.api
          .sendMessage(conversation.id, contenu, {
            type: 'fichier',
            fichierNom: result.nom,
            fichierUrl: result.url,
            fichierTaille: result.taille
          })
          .subscribe({
            next: (message) => {
              this.sending.set(false);
              this.messages.update((items) => [...items, message]);
              this.preloadImageAttachments([message]);
              this.conversations.update((list) =>
                list.map((c) =>
                  c.id === conversation.id
                    ? {
                        ...c,
                        dernierMessageContenu: message.contenu,
                        dernierMessageAt: message.date,
                        dernierMessageSenderId: message.senderId,
                        dernierMessageSenderNom: extrairePrenom(this.currentUser()?.nom)
                      }
                    : c
                )
              );
              this.scrollThread();
            },
            error: (err) => {
              this.sending.set(false);
              this.error.set(this.formatErrorMessage(err, "Échec de l'envoi du fichier."));
            }
          });
        input.value = '';
      },
      error: (err) => {
        this.sending.set(false);
        this.error.set(this.formatErrorMessage(err, "Échec de l'envoi du fichier."));
        input.value = '';
      }
    });
  }

  isImageFile(nom: string | null | undefined): boolean {
    if (!nom) return false;
    const ext = nom.split('.').pop()?.toLowerCase() ?? '';
    return ext === 'jpg' || ext === 'jpeg' || ext === 'png';
  }

  downloadProtectedFile(message: Message): void {
    if (!message.fichierUrl) return;
    const url = message.fichierUrl;
    const existing = this.rawBlobUrls.get(url);
    if (existing) {
      this.triggerBrowserDownload(existing, message.fichierNom || 'fichier');
      return;
    }
    this.api.loadFileBlob(url).subscribe({
      next: (blob) => {
        const objUrl = URL.createObjectURL(blob);
        this.rawBlobUrls.set(url, objUrl);
        this.filePreviewUrls.update((m) => ({
          ...m,
          [url]: this.sanitizer.bypassSecurityTrustUrl(objUrl)
        }));
        this.triggerBrowserDownload(objUrl, message.fichierNom || 'fichier');
      },
      error: (err) => {
        this.error.set(this.formatErrorMessage(err, 'Impossible de télécharger le fichier.'));
      }
    });
  }

  private triggerBrowserDownload(blobUrl: string, filename: string): void {
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = filename;
    a.rel = 'noopener';
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  private preloadImageAttachments(messages: Message[]): void {
    for (const msg of messages) {
      if (
        msg.fichierUrl &&
        this.isImageFile(msg.fichierNom) &&
        !this.rawBlobUrls.has(msg.fichierUrl) &&
        !this.loadingBlobs.has(msg.fichierUrl)
      ) {
        const url = msg.fichierUrl;
        this.loadingBlobs.add(url);
        this.api.loadFileBlob(url).subscribe({
          next: (blob) => {
            this.loadingBlobs.delete(url);
            const objUrl = URL.createObjectURL(blob);
            this.rawBlobUrls.set(url, objUrl);
            this.filePreviewUrls.update((m) => ({
              ...m,
              [url]: this.sanitizer.bypassSecurityTrustUrl(objUrl)
            }));
          },
          error: () => {
            this.loadingBlobs.delete(url);
          }
        });
      }
    }
  }

  onComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onTyping(): void {
    if (!this.selected()) return;
    if (!this.draft().trim()) {
      this.typingUser.set(null);
      return;
    }
    this.typingUser.set(extrairePrenom(this.currentUser()?.nom));
    window.clearTimeout(this.typingTimer);
    this.typingTimer = window.setTimeout(() => this.typingUser.set(null), 2000);
  }

  onThreadScroll(): void {
    const el = this.thread?.nativeElement;
    if (!el) return;
    const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
    this.showScrollBottom.set(distance > 80);
  }

  scrollToBottom(): void {
    const el = this.thread?.nativeElement;
    if (!el) return;
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
    this.showScrollBottom.set(false);
  }

  startEdit(message: Message): void {
    this.editingMessageId.set(message.id);
    this.editingContent.set(message.contenu);
  }

  cancelEdit(): void {
    this.editingMessageId.set(null);
    this.editingContent.set('');
  }

  saveEdit(message: Message): void {
    const contenu = this.editingContent().trim();
    if (!contenu) {
      this.error.set('Le message ne peut pas être vide.');
      return;
    }

    this.savingEdit.set(true);
    this.api.updateMessage(message.id, contenu).subscribe({
      next: (updated) => {
        this.messages.update((items) =>
          items.map((item) => (item.id === updated.id ? updated : item))
        );
        this.editingMessageId.set(null);
        this.savingEdit.set(false);
        this.flash('Message modifié');
        this.refreshQuietly();
      },
      error: (err) => {
        this.savingEdit.set(false);
        this.error.set(this.formatErrorMessage(err, 'Modification du message impossible.'));
      }
    });
  }

  requestDeleteMessage(message: Message): void {
    this.rememberFocus();
    this.confirmDeleteDialog.set({
      type: 'message',
      id: message.id,
      title: 'Supprimer ce message ?',
      message: 'Voulez-vous vraiment supprimer définitivement ce message ? Cette action est irréversible.',
      confirmLabel: 'Supprimer',
      danger: true
    });
  }

  cancelDelete(): void {
    if (this.deleting()) return;
    if (this.confirmDeleteDialog()?.type === 'retirer_participant') {
      this.panelPendingRemoval.set(null);
      const conv = this.selected();
      if (conv) this.panelParticipants.set(this.buildParticipantUsers(conv));
    }
    this.confirmDeleteDialog.set(null);
    this.restoreFocus();
  }

  executeDelete(): void {
    const dialog = this.confirmDeleteDialog();
    if (!dialog) return;

    if (dialog.type === 'retirer_participant') {
      const pending = this.panelPendingRemoval();
      this.confirmDeleteDialog.set(null);
      this.panelPendingRemoval.set(null);
      this.restoreFocus();
      if (pending) {
        const conv = this.selected();
        if (conv) {
          this.panelParticipants.set(pending.nextList);
          this.sauvegarderParticipants(
            pending.nextList,
            this.isGroupConv(conv) ? (conv.nomGroupe ?? null) : null,
            this.isGroupConv(conv) ? 'groupe' : 'direct'
          );
        }
      }
      return;
    }

    if (dialog.type === 'quitter') {
      this.deleting.set(true);
      this.api.quitterGroupe(dialog.id).subscribe({
        next: () => {
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          this.conversations.update((items) => items.filter((c) => c.id !== dialog.id));
          if (this.selected()?.id === dialog.id) {
            this.selected.set(null);
            this.messages.set([]);
            this.mobileThread.set(false);
            this.showParticipantsPanel.set(false);
          }
          this.flash('Vous avez quitté le groupe');
          this.restoreFocus();
        },
        error: (err) => {
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          if (err?.status === 400 && String(err?.error?.message ?? '').includes('moins de 2')) {
            this.showConvertDirectPrompt.set(true);
            this.showParticipantsPanel.set(true);
            return;
          }
          this.error.set(this.formatErrorMessage(err, 'Impossible de quitter le groupe.'));
          this.restoreFocus();
        }
      });
      return;
    }

    this.deleting.set(true);

    if (dialog.type === 'conversation') {
      this.api.deleteConversation(dialog.id).subscribe({
        next: () => {
          this.conversations.update((items) => items.filter((item) => item.id !== dialog.id));
          if (this.selected()?.id === dialog.id) {
            this.selected.set(null);
            this.messages.set([]);
            this.mobileThread.set(false);
            this.showParticipantsPanel.set(false);
          }
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          this.flash('Conversation supprimée');
          this.restoreFocus();
        },
        error: (err) => {
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          this.error.set(this.formatErrorMessage(err, 'Suppression de la conversation impossible.'));
          this.restoreFocus();
        }
      });
    } else if (dialog.type === 'message') {
      this.api.deleteMessage(dialog.id).subscribe({
        next: () => {
          this.messages.update((items) => items.filter((item) => item.id !== dialog.id));
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          this.flash('Message supprimé');
          this.refreshQuietly();
          this.restoreFocus();
        },
        error: (err) => {
          this.deleting.set(false);
          this.confirmDeleteDialog.set(null);
          this.error.set(this.formatErrorMessage(err, 'Suppression du message impossible.'));
          this.restoreFocus();
        }
      });
    }
  }

  // ── Focus trap pour modales ──
  onModalKeydown(event: KeyboardEvent, container: HTMLElement | undefined): void {
    if (event.key !== 'Tab' || !container) return;
    const focusables = Array.from(
      container.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
    ).filter((el) => el.offsetParent !== null);

    if (!focusables.length) return;
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    const active = document.activeElement as HTMLElement | null;

    if (event.shiftKey) {
      if (!active || active === first || !container.contains(active)) {
        event.preventDefault();
        last.focus();
      }
    } else if (!active || active === last || !container.contains(active)) {
      event.preventDefault();
      first.focus();
    }
  }

  // ── Helpers d'affichage ──
  isGroupConv(conversation: Conversation | null | undefined): boolean {
    if (!conversation) return false;
    return Boolean(conversation.estGroupe) || String(conversation.type ?? '').toLowerCase() === 'groupe';
  }

  isMine(message: Message): boolean {
    return !this.isSysteme(message) && message.senderId === this.currentUser()?.id;
  }

  isSysteme(message: Message): boolean {
    return message.type === 'systeme';
  }

  canManage(conversation: Conversation): boolean {
    const user = this.currentUser();
    return !!user && (user.id === conversation.candidatUserId || user.role === 'ADMIN');
  }

  canQuitter(conversation: Conversation): boolean {
    return this.isGroupConv(conversation);
  }

  canSupprimer(conversation: Conversation): boolean {
    const user = this.currentUser();
    return !!user && (user.id === conversation.candidatUserId || user.role === 'ADMIN');
  }

  displayName(conversation: Conversation): string {
    if (this.isGroupConv(conversation) && conversation.nomGroupe) {
      return conversation.nomGroupe;
    }
    const myId = this.currentUser()?.id;
    const usersMap = this.resolvedUsers();
    const otherIds = conversation.participantIds.filter((id) => id !== myId);
    const participants = conversation.participants ?? [];

    if (otherIds.length) {
      return otherIds
        .map((id) => {
          const p = participants.find((item) => item.id === id);
          if (p?.nom && !p.nom.startsWith('Utilisateur #') && p.nom !== 'Membre') {
            return p.nom;
          }
          const resolved = usersMap[id];
          if (resolved?.nom) return resolved.nom;
          return p?.nom && !p.nom.startsWith('Utilisateur #') ? p.nom : 'Membre';
        })
        .join(', ');
    }

    const others = this.others(conversation);
    if (others.length) {
      return others.map((p) => p.nom).join(', ');
    }
    return 'Conversation';
  }

  senderFirstName(message: Message): string {
    if (message.senderNom && !message.senderNom.startsWith('Utilisateur #')) {
      return extrairePrenom(message.senderNom);
    }
    const conv = this.selected();
    const p = conv?.participants?.find((item) => item.id === message.senderId);
    if (p?.nom && !p.nom.startsWith('Utilisateur #')) {
      return extrairePrenom(p.nom);
    }
    const resolved = this.resolvedUsers()[message.senderId];
    if (resolved?.nom) {
      return extrairePrenom(resolved.nom);
    }
    return 'Membre';
  }

  couleurPuce(id: number): PucePalette {
    return couleurPourUtilisateur(id);
  }

  directAvatarBg(conversation: Conversation): string {
    const myId = this.currentUser()?.id;
    const otherId = conversation.participantIds.find((id) => id !== myId) ?? conversation.candidatUserId ?? 0;
    return this.couleurPuce(otherId).text;
  }

  stackedGroupAvatars(conversation: Conversation): { id: number; initiales: string; bg: string }[] {
    const participants = conversation.participants ?? [];
    const usersMap = this.resolvedUsers();
    const orderedIds = [...conversation.participantIds];
    const creatorIdx = orderedIds.indexOf(conversation.candidatUserId);
    if (creatorIdx > 0) {
      orderedIds.splice(creatorIdx, 1);
      orderedIds.unshift(conversation.candidatUserId);
    }

    return orderedIds.slice(0, 2).map((id) => {
      const p = participants.find((item) => item.id === id);
      const nom = p?.nom && !p.nom.startsWith('Utilisateur #')
        ? p.nom
        : usersMap[id]?.nom ?? (id === this.currentUser()?.id ? this.currentUser()!.nom : 'Membre');
      const bg = id === conversation.candidatUserId ? '#0F6E56' : this.couleurPuce(id).text;
      return {
        id,
        initiales: formaterInitiales(nom),
        bg
      };
    });
  }

  headerGroupInitials(conversation: Conversation): string {
    if (conversation.nomGroupe?.trim()) {
      return formaterInitiales(conversation.nomGroupe);
    }
    return 'GR';
  }

  initials(nameOrConv: string | Conversation): string {
    const name = typeof nameOrConv === 'string' ? nameOrConv : this.displayName(nameOrConv);
    return formaterInitiales(name);
  }

  recipientRole(conversation: Conversation): string {
    if (this.isGroupConv(conversation)) {
      return `Groupe · ${conversation.participantIds.length} participants`;
    }
    const myId = this.currentUser()?.id;
    const usersMap = this.resolvedUsers();
    const firstOtherId = conversation.participantIds.find((id) => id !== myId);
    const p = (conversation.participants ?? []).find((item) => item.id === firstOtherId);
    const role = p?.role ?? (firstOtherId ? usersMap[firstOtherId]?.role : null);
    return formaterRoleLibelle(role);
  }

  roleLibelle(role: string | null | undefined): string {
    return formaterRoleLibelle(role);
  }

  preview(conversation: Conversation): string {
    const text = conversation.dernierMessageContenu?.trim();
    if (!text) return 'Aucun message';
    if (this.isGroupConv(conversation) && conversation.dernierMessageSenderId) {
      const senderFirst = conversation.dernierMessageSenderNom
        ? extrairePrenom(conversation.dernierMessageSenderNom)
        : this.resolveFirstName(conversation, conversation.dernierMessageSenderId);
      if (senderFirst && !text.startsWith(`${senderFirst} a `)) {
        return `${senderFirst} : ${text}`;
      }
    }
    return text;
  }

  formatDate(iso: string | null): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const now = new Date();
    const sameDay =
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate();
    if (sameDay) {
      return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    }
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    if (
      d.getFullYear() === yesterday.getFullYear() &&
      d.getMonth() === yesterday.getMonth() &&
      d.getDate() === yesterday.getDate()
    ) {
      return 'Hier';
    }
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  formatMessageTime(iso: string | null): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  shouldShowDateDivider(list: Message[], index: number): boolean {
    if (index === 0) return false;
    const current = new Date(list[index].date);
    const prev = new Date(list[index - 1].date);
    return (
      current.getFullYear() !== prev.getFullYear() ||
      current.getMonth() !== prev.getMonth() ||
      current.getDate() !== prev.getDate()
    );
  }

  getDateDivider(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    if (
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
    ) {
      return "Aujourd'hui";
    }
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  hasConversationWith(participantId: number): boolean {
    const currentId = this.currentUser()?.id;
    return !!currentId && !!this.findExistingConversation(currentId, participantId);
  }

  private resolveFirstName(conv: Conversation, userId: number): string {
    if (userId === this.currentUser()?.id) {
      return extrairePrenom(this.currentUser()?.nom);
    }
    const p = (conv.participants ?? []).find((item) => item.id === userId);
    if (p?.nom && !p.nom.startsWith('Utilisateur #')) {
      return extrairePrenom(p.nom);
    }
    const resolved = this.resolvedUsers()[userId];
    if (resolved?.nom) {
      return extrairePrenom(resolved.nom);
    }
    return '';
  }

  private buildParticipantUsers(conv: Conversation): UserSummary[] {
    const usersMap = this.resolvedUsers();
    const myId = this.currentUser()?.id;
    const participants = conv.participants ?? [];

    const mapped = conv.participantIds.map((id) => {
      if (id === myId && this.currentUser()) {
        return {
          id,
          nom: this.currentUser()!.nom,
          role: this.currentUser()!.role
        };
      }
      const p = participants.find((item) => item.id === id);
      if (p?.nom && !p.nom.startsWith('Utilisateur #') && p.nom !== 'Membre') {
        return { id, nom: p.nom, role: p.role };
      }
      const cached = usersMap[id];
      if (cached) return cached;
      return { id, nom: 'Membre', role: p?.role ?? null };
    });

    return mapped.sort((a, b) => {
      if (a.id === conv.candidatUserId) return -1;
      if (b.id === conv.candidatUserId) return 1;
      return 0;
    });
  }

  private others(conversation: Conversation): ParticipantSummary[] {
    const myId = this.currentUser()?.id;
    return (conversation.participants ?? []).filter((p) => p.id !== myId);
  }

  private loadMessages(conversationId: string, markRead: boolean): void {
    this.messagesLoading.set(true);
    this.api.getMessages(conversationId).subscribe({
      next: (data) => {
        if (this.selected()?.id !== conversationId) return;
        this.messages.set(data);
        this.messagesLoading.set(false);
        this.preloadImageAttachments(data);
        this.scrollThread();
        if (markRead) this.markIncomingAsRead(data);
      },
      error: (err) => {
        this.messagesLoading.set(false);
        this.error.set(this.formatErrorMessage(err, 'Impossible de charger les messages.'));
      }
    });
  }

  private markIncomingAsRead(data: Message[]): void {
    const unread = data.filter(
      (m) => !this.isSysteme(m) && !m.lu && m.senderId !== this.currentUser()?.id
    );
    if (!unread.length) return;

    unread.forEach((message) => {
      this.api.markAsRead(message.id).subscribe({
        next: (updated) => {
          this.messages.update((items) =>
            items.map((item) => (item.id === updated.id ? updated : item))
          );
          this.conversations.update((list) =>
            list.map((c) =>
              c.id === message.conversationId
                ? { ...c, nonLusCount: Math.max(0, (c.nonLusCount ?? 1) - 1) }
                : c
            )
          );
        },
        error: () => {}
      });
    });
  }

  private refreshQuietly(): void {
    this.api.getConversations().subscribe({
      next: (data) => {
        this.conversations.set(data);
        this.syncSelected(data);
        this.enrichMissingParticipants(data);
      },
      error: () => {}
    });

    const selected = this.selected();
    if (!selected || this.editingMessageId()) return;

    this.api.getMessages(selected.id).subscribe({
      next: (data) => {
        if (this.selected()?.id !== selected.id) return;
        const current = this.messages();
        const changed =
          current.length !== data.length ||
          current.at(-1)?.id !== data.at(-1)?.id ||
          current.at(-1)?.contenu !== data.at(-1)?.contenu ||
          current.some((m, idx) => m.lu !== data[idx]?.lu);

        if (!changed) return;

        const hadNewIncoming = data.some(
          (m) =>
            !this.isSysteme(m) &&
            !m.lu &&
            m.senderId !== this.currentUser()?.id &&
            !current.some((item) => item.id === m.id)
        );

        this.messages.set(data);
        this.preloadImageAttachments(data);
        if (!this.showScrollBottom()) {
          this.scrollThread();
        }
        if (hadNewIncoming) this.markIncomingAsRead(data);
      },
      error: () => {}
    });
  }

  private syncSelected(data: Conversation[]): void {
    const selected = this.selected();
    if (!selected) return;
    const updated = data.find((c) => c.id === selected.id);
    if (updated) this.selected.set(updated);
  }

  private findExistingConversation(currentId: number, participantId: number): Conversation | undefined {
    return this.conversations().find((c) => {
      if (this.isGroupConv(c)) return false;
      const ids = [...c.participantIds].sort((a, b) => a - b);
      return (
        ids.length === 2 &&
        ids[0] === Math.min(currentId, participantId) &&
        ids[1] === Math.max(currentId, participantId)
      );
    });
  }

  private openExistingConversation(currentId: number, participantId: number): void {
    this.api.getConversations().subscribe({
      next: (data) => {
        this.conversations.set(data);
        const existing = this.findExistingConversation(currentId, participantId);
        if (existing) {
          this.showNewModal.set(false);
          this.recipientSelection.set([]);
          this.flash('Conversation existante ouverte');
          this.selectConversation(existing);
        } else {
          this.error.set('Cette conversation existe déjà.');
        }
      },
      error: (err) => {
        this.error.set(this.formatErrorMessage(err, 'Cette conversation existe déjà.'));
      }
    });
  }

  private scrollThread(): void {
    window.setTimeout(() => {
      const el = this.thread?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
        this.showScrollBottom.set(false);
      }
    }, 50);
  }

  private flash(msg: string): void {
    this.success.set(msg);
    this.error.set('');
    window.clearTimeout(this.successTimer);
    this.successTimer = window.setTimeout(() => {
      if (this.success() === msg) this.success.set('');
    }, 4000);
  }

  private formatErrorMessage(
    error: { status?: number; error?: { message?: string; detail?: string } | string; message?: string },
    fallback: string
  ): string {
    if (error?.status === 0) {
      return 'Impossible de joindre le serveur.';
    }
    const body = error?.error;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object') return body.message ?? body.detail ?? fallback;
    return fallback;
  }

  private enrichMissingParticipants(conversations: Conversation[]): void {
    const myId = this.currentUser()?.id;
    const idsToLookup = new Set<number>();

    for (const conv of conversations) {
      const participants = conv.participants ?? [];
      for (const id of conv.participantIds) {
        if (id === myId) continue;
        const p = participants.find((item) => item.id === id);
        if (p?.nom && !p.nom.startsWith('Utilisateur #') && p.nom !== 'Membre') {
          this.directoryCache.set(id, { id, nom: p.nom, role: p.role });
          continue;
        }
        if (!this.directoryCache.has(id) && !this.directoryPending.has(id)) {
          idsToLookup.add(id);
        }
      }
    }

    idsToLookup.forEach((id) => {
      this.directoryPending.add(id);
      this.api.lookupUser(id).subscribe({
        next: (user) => {
          this.directoryPending.delete(id);
          this.directoryCache.set(id, user);
          this.resolvedUsers.update((map) => ({ ...map, [id]: user }));
        },
        error: () => {
          this.directoryPending.delete(id);
        }
      });
    });
  }

  private rememberFocus(): void {
    this.lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  }

  private restoreFocus(): void {
    const target = this.lastFocusedElement;
    this.lastFocusedElement = null;
    if (target && typeof target.focus === 'function') {
      window.setTimeout(() => target.focus(), 0);
    }
  }

  private focusFirstInModal(container: HTMLElement | undefined): void {
    if (!container) return;
    const first = container.querySelector<HTMLElement>(
      'input:not([disabled]), button:not([disabled]), textarea:not([disabled])'
    );
    first?.focus();
  }
}
