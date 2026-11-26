import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  EventEmitter,
  HostListener,
  OnDestroy,
  OnInit,
  Output,
  computed,
  inject,
  input,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdminConversationDetail,
  AdminConversationSummary,
  MessagerieAdminStats,
  SignalementActionAdmin,
  SignalementItem,
  SupervisionScope,
  formaterRoleLibelle
} from '../../messagerie/messagerie.models';
import { MessagerieService } from '../../messagerie/messagerie.service';

@Component({
  selector: 'app-messagerie-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messagerie-admin.html',
  styleUrl: './messagerie-admin.css'
})
export class MessagerieAdmin implements OnInit, OnDestroy {
  private readonly messagerieService = inject(MessagerieService);

  readonly initialScope = input<SupervisionScope>('TOUTES');
  readonly navTick = input(0);
  @Output() toast = new EventEmitter<string>();

  readonly scopes: readonly { key: SupervisionScope; label: string }[] = [
    { key: 'TOUTES',    label: 'Toutes'    },
    { key: 'GROUPES',   label: 'Groupes'   },
    { key: 'SIGNALEES', label: 'Signalées' },
    { key: 'ARCHIVEES', label: 'Archivées' }
  ];

  readonly actions: readonly { value: SignalementActionAdmin; label: string }[] = [
    { value: 'IGNORER',         label: 'Ignorer le signalement' },
    { value: 'AVERTIR',         label: "Avertir l'utilisateur"  },
    { value: 'MASQUER_MESSAGE', label: 'Masquer le message'      },
    { value: 'SUSPENDRE_ENVOI', label: "Suspendre l'envoi"       }
  ];

  // ── Filtres et pagination ──────────────────────────────────────
  readonly scope = signal<SupervisionScope>('TOUTES');
  readonly query = signal('');
  readonly page  = signal(0);
  readonly size  = 10;

  // ── Stats du serveur ───────────────────────────────────────────
  readonly serverStats = signal<MessagerieAdminStats>({
    conversations: 0,
    groupes: 0,
    messagesAujourdhui: 0,
    signalementsATraiter: 0,
    nouvellesConversationsSemaine: 0,
    nouveauxGroupesSemaine: 0,
    heurePicActivite: null,
    heurePicLibelle: 'Aucune activité'
  });

  /** Toutes les conversations chargées */
  readonly allRows = signal<AdminConversationSummary[]>([]);

  // ── UI States ──────────────────────────────────────────────────
  readonly loading        = signal(false);
  readonly refreshing     = signal(false);
  readonly error          = signal('');
  readonly lastRefreshed  = signal<Date | null>(null);
  readonly selectedDetail = signal<AdminConversationDetail | null>(null);
  readonly detailLoading  = signal(false);
  readonly detailBusy     = signal(false);
  readonly signalementActionMap = signal<Record<string, SignalementActionAdmin>>({});
  readonly signalementNoteMap   = signal<Record<string, string>>({});
  readonly openMenuId     = signal<string | null>(null);

  private focusSignalements = false;
  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private autoRefreshTimer: ReturnType<typeof setInterval> | null = null;

  // ── Helpers de classification ──────────────────────────────────
  isGroupe(row: AdminConversationSummary): boolean {
    return (
      String(row.type ?? '').toLowerCase() === 'groupe' ||
      (row.membres != null && row.membres > 2) ||
      (row.participants != null && row.participants.length > 2) ||
      (row.conversation != null && row.conversation.includes(' / ') && row.conversation.split(' / ').length > 2)
    );
  }

  isSignalee(row: AdminConversationSummary): boolean {
    return (
      Boolean(row.signalee) ||
      String(row.statut ?? '').toUpperCase() === 'SIGNALEE' ||
      (row.signalementsCount != null && row.signalementsCount > 0)
    );
  }

  isArchivee(row: AdminConversationSummary): boolean {
    return (
      Boolean(row.archivee) ||
      String(row.statut ?? '').toUpperCase() === 'ARCHIVEE'
    );
  }

  // ── KPI dynamiques ─────────────────────────────────────────────
  readonly kpiConversations = computed(() => {
    const fromRows = this.allRows().length;
    return fromRows > 0 ? fromRows : this.serverStats().conversations;
  });

  readonly kpiGroupes = computed(() => {
    const fromRows = this.allRows().filter(r => this.isGroupe(r)).length;
    return fromRows > 0 ? fromRows : this.serverStats().groupes;
  });

  readonly kpiMessages = computed(() => this.serverStats().messagesAujourdhui);

  readonly kpiSignalements = computed(() => {
    const fromRows = this.allRows().filter(r => this.isSignalee(r)).length;
    return fromRows > 0 ? fromRows : this.serverStats().signalementsATraiter;
  });

  // ── Badges des onglets ─────────────────────────────────────────
  scopeCount(key: SupervisionScope): number {
    const rows = this.allRows();
    switch (key) {
      case 'TOUTES':    return rows.length;
      case 'GROUPES':   return rows.filter(r => this.isGroupe(r)).length;
      case 'SIGNALEES': return rows.filter(r => this.isSignalee(r)).length;
      case 'ARCHIVEES': return rows.filter(r => this.isArchivee(r)).length;
      default:          return 0;
    }
  }

  // ── Filtrage dynamique instantané (computed) ───────────────────
  readonly filteredRows = computed(() => {
    const s = this.scope();
    const q = this.query().trim().toLowerCase();
    let result = this.allRows();

    // Filtre par portée / onglet
    switch (s) {
      case 'GROUPES':
        result = result.filter(r => this.isGroupe(r));
        break;
      case 'SIGNALEES':
        result = result.filter(r => this.isSignalee(r));
        break;
      case 'ARCHIVEES':
        result = result.filter(r => this.isArchivee(r));
        break;
      case 'TOUTES':
      default:
        // Toutes les conversations
        break;
    }

    // Filtre de recherche textuelle
    if (q) {
      result = result.filter(r =>
        r.conversation.toLowerCase().includes(q) ||
        r.participants?.some(p => p.nom.toLowerCase().includes(q)) ||
        (r.dernierMessageContenu && r.dernierMessageContenu.toLowerCase().includes(q))
      );
    }

    return result;
  });

  /** Découpage de la page courante */
  readonly pagedRows = computed(() => {
    const start = this.page() * this.size;
    return this.filteredRows().slice(start, start + this.size);
  });

  readonly totalElements = computed(() => this.filteredRows().length);
  readonly totalPages    = computed(() => Math.max(1, Math.ceil(this.filteredRows().length / this.size)));

  readonly pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly rangeLabel = computed(() => {
    const total = this.totalElements();
    if (total === 0) return 'Aucune conversation';
    const start = this.page() * this.size + 1;
    const end   = Math.min(total, start + this.pagedRows().length - 1);
    return `Affichage de ${start} à ${end} sur ${total}`;
  });

  // ── Lifecycle ──────────────────────────────────────────────────
  ngOnInit(): void {
    const init = this.initialScope();
    if (init) this.scope.set(init);
    this.loadSupervision();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.searchTimer)      clearTimeout(this.searchTimer);
    if (this.autoRefreshTimer) clearInterval(this.autoRefreshTimer);
  }

  // ── Événements globaux ─────────────────────────────────────────
  @HostListener('document:click')
  onDocumentClick(): void {
    this.openMenuId.set(null);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.openMenuId.set(null);
    this.closeDetail();
  }

  // ── Chargement des données (dynamique via API) ─────────────────
  loadSupervision(silent = false): void {
    if (silent) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }
    this.error.set('');

    // Charge TOUTES les données (size=500) pour alimenter le filtrage client instantané
    this.messagerieService
      .getAdminSupervision('TOUTES', '', 0, 500)
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          this.refreshing.set(false);
          if (res.stats) {
            this.serverStats.set(res.stats);
          }
          const convs = res.conversations ?? [];
          this.allRows.set(convs);
          this.lastRefreshed.set(new Date());

          // Ajuste la page si hors bornes
          if (this.page() >= this.totalPages()) {
            this.page.set(0);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.refreshing.set(false);
          this.error.set(this.readError(err));
        }
      });
  }

  // ── Actions utilisateur ────────────────────────────────────────
  setScope(scope: SupervisionScope): void {
    this.scope.set(scope);
    this.page.set(0);
  }

  onSearchChange(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  clearSearch(): void {
    this.query.set('');
    this.page.set(0);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.page()) return;
    this.page.set(page);
  }

  toggleMenu(id: string, event: Event): void {
    event.stopPropagation();
    this.openMenuId.update(c => c === id ? null : id);
  }

  openSignalements(row: AdminConversationSummary, event?: Event): void {
    event?.stopPropagation();
    this.focusSignalements = true;
    this.openDetail(row);
  }

  openDetail(row: AdminConversationSummary): void {
    this.openMenuId.set(null);
    this.detailLoading.set(true);
    const scrollToReports = this.focusSignalements;

    this.messagerieService.getAdminConversationDetail(row.id).subscribe({
      next: (detail) => {
        this.detailLoading.set(false);
        this.selectedDetail.set(detail);
        this.initSignalementControls(detail.signalements ?? []);
        if (scrollToReports) {
          this.focusSignalements = false;
          setTimeout(() =>
            document.getElementById('ma-signalements')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
          );
        }
      },
      error: (err: HttpErrorResponse) => {
        this.detailLoading.set(false);
        this.focusSignalements = false;
        this.toast.emit(this.readError(err));
      }
    });
  }

  closeDetail(): void {
    if (this.detailBusy()) return;
    this.selectedDetail.set(null);
  }

  toggleArchive(row: AdminConversationSummary, event?: Event): void {
    event?.stopPropagation();
    this.openMenuId.set(null);
    const newArchivee = !this.isArchivee(row);
    this.detailBusy.set(true);

    // Mise à jour optimiste immédiate dans allRows
    this.allRows.update(rows =>
      rows.map(r => r.id === row.id ? { ...r, archivee: newArchivee, statut: newArchivee ? 'archivee' : 'active' } : r)
    );

    this.messagerieService.updateAdminConversationModeration(row.id, { archivee: newArchivee }).subscribe({
      next: (updated) => {
        this.detailBusy.set(false);
        this.toast.emit(newArchivee ? 'Conversation archivée.' : 'Conversation désarchivée.');
        this.allRows.update(rows =>
          rows.map(r => r.id === updated.id ? { ...r, ...updated, archivee: newArchivee } : r)
        );
        const curDetail = this.selectedDetail();
        if (curDetail && curDetail.conversation.id === row.id) {
          this.selectedDetail.set({
            ...curDetail,
            conversation: { ...curDetail.conversation, archivee: newArchivee, statut: newArchivee ? 'archivee' : 'active' }
          });
        }
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
        // En cas d'erreur, revert
        this.allRows.update(rows =>
          rows.map(r => r.id === row.id ? { ...r, archivee: !newArchivee } : r)
        );
        this.toast.emit(this.readError(err));
      }
    });
  }

  consulterSignalement(sig: SignalementItem): void {
    this.detailBusy.set(true);
    this.messagerieService.consulterSignalement(sig.id).subscribe({
      next: (revealed) => {
        this.detailBusy.set(false);
        const current = this.selectedDetail();
        if (!current) return;
        const updatedSigs = (current.signalements ?? []).map(item =>
          item.id === revealed.id ? revealed : item
        );
        this.selectedDetail.set({ ...current, signalements: updatedSigs });
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
        this.toast.emit(this.readError(err));
      }
    });
  }

  setSignalementAction(sigId: string, action: string): void {
    this.signalementActionMap.update(map => ({ ...map, [sigId]: action as SignalementActionAdmin }));
  }

  setSignalementNote(sigId: string, note: string): void {
    this.signalementNoteMap.update(map => ({ ...map, [sigId]: note }));
  }

  traiterSignalement(sig: SignalementItem): void {
    const action = this.signalementActionMap()[sig.id] ?? 'IGNORER';
    const note   = this.signalementNoteMap()[sig.id]   ?? '';
    this.detailBusy.set(true);

    this.messagerieService.traiterSignalement(sig.id, action, note).subscribe({
      next: () => {
        this.detailBusy.set(false);
        this.toast.emit('Signalement traité avec succès.');
        this.loadSupervision(true);
        const detail = this.selectedDetail();
        if (detail) this.openDetail(detail.conversation);
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
        this.toast.emit(this.readError(err));
      }
    });
  }

  // ── Exports ────────────────────────────────────────────────────
  exportCsv(): void {
    this.messagerieService.exportAdminSupervisionCsv(this.scope(), this.query()).subscribe({
      next: (blob) => this.downloadCsvBlob(blob, 'messagerie-supervision.csv'),
      error: (err: HttpErrorResponse) => this.toast.emit(this.readError(err))
    });
  }

  exportSingleCsv(convId: string, event?: Event): void {
    event?.stopPropagation();
    this.openMenuId.set(null);
    this.messagerieService.exportAdminConversationCsv(convId).subscribe({
      next: (blob) => {
        this.downloadCsvBlob(blob, `conversation-${convId}.csv`);
      },
      error: (err: HttpErrorResponse) => this.toast.emit(this.readError(err))
    });
  }

  // ── Formatage d'affichage ──────────────────────────────────────
  formatDateTime(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatTimeAgo(date: Date | null): string {
    if (!date) return '';
    const diff = Math.floor((Date.now() - date.getTime()) / 1000);
    if (diff < 60) return "à l'instant";
    if (diff < 3600) return `il y a ${Math.floor(diff / 60)} min`;
    return `il y a ${Math.floor(diff / 3600)} h`;
  }

  roleLabel(role: string | null | undefined): string {
    return formaterRoleLibelle(role);
  }

  statutTone(row: AdminConversationSummary): string {
    if (this.isSignalee(row)) return 'red';
    if (this.isArchivee(row)) return 'gray';
    return 'green';
  }

  statutLabel(row: AdminConversationSummary): string {
    if (this.isSignalee(row)) return 'Signalée';
    if (this.isArchivee(row)) return 'Archivée';
    return 'Active';
  }

  // ── Privé ──────────────────────────────────────────────────────
  private startAutoRefresh(): void {
    this.autoRefreshTimer = setInterval(() => {
      if (!this.selectedDetail() && !this.loading()) {
        this.loadSupervision(true);
      }
    }, 30000);
  }

  private initSignalementControls(items: SignalementItem[]): void {
    const actions: Record<string, SignalementActionAdmin> = {};
    const notes:   Record<string, string> = {};
    for (const s of items) {
      actions[s.id] = (s.actionAdmin as SignalementActionAdmin) || 'IGNORER';
      notes[s.id]   = '';
    }
    this.signalementActionMap.set(actions);
    this.signalementNoteMap.set(notes);
  }

  private readError(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Impossible de joindre le serveur de messagerie.';
    return err.error?.message ?? err.error?.detail ?? 'Service messagerie indisponible pour le moment.';
  }

  private downloadCsvBlob(blob: Blob, filename: string): void {
    const url  = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
}
