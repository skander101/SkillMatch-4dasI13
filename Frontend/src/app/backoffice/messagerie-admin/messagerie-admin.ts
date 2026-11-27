import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  EventEmitter,
  HostListener,
  Output,
  computed,
  effect,
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
export class MessagerieAdmin {
  private readonly messagerieService = inject(MessagerieService);

  readonly initialScope = input<SupervisionScope>('TOUTES');
  readonly navTick = input(0);
  @Output() toast = new EventEmitter<string>();

  readonly scopes: readonly { key: SupervisionScope; label: string }[] = [
    { key: 'TOUTES', label: 'Toutes' },
    { key: 'GROUPES', label: 'Groupes' },
    { key: 'SIGNALEES', label: 'Signalées' },
    { key: 'ARCHIVEES', label: 'Archivées' }
  ];

  readonly actions: readonly { value: SignalementActionAdmin; label: string }[] = [
    { value: 'IGNORER', label: 'Ignorer le signalement' },
    { value: 'AVERTIR', label: "Avertir l'utilisateur" },
    { value: 'MASQUER_MESSAGE', label: 'Masquer le message' },
    { value: 'SUSPENDRE_ENVOI', label: "Suspendre l'envoi" }
  ];

  readonly scope = signal<SupervisionScope>('TOUTES');
  readonly query = signal('');
  readonly page = signal(0);
  readonly size = 10;

  readonly stats = signal<MessagerieAdminStats>({
    conversations: 0,
    groupes: 0,
    messagesAujourdhui: 0,
    signalementsATraiter: 0,
    nouvellesConversationsSemaine: 0,
    nouveauxGroupesSemaine: 0,
    heurePicActivite: null,
    heurePicLibelle: 'Aucune activité'
  });

  readonly rows = signal<AdminConversationSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly selectedDetail = signal<AdminConversationDetail | null>(null);
  readonly detailLoading = signal(false);
  readonly detailBusy = signal(false);
  readonly signalementActionMap = signal<Record<string, SignalementActionAdmin>>({});
  readonly signalementNoteMap = signal<Record<string, string>>({});
  readonly openMenuId = signal<string | null>(null);
  private focusSignalements = false;

  readonly pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, idx) => idx)
  );

  readonly rangeLabel = computed(() => {
    const total = this.totalElements();
    if (total === 0) return 'Aucune conversation';
    const start = this.page() * this.size + 1;
    const end = Math.min(total, start + this.rows().length - 1);
    return `Affichage de ${start} à ${end} sur ${total}`;
  });

  constructor() {
    effect(() => {
      const nextScope = this.initialScope();
      this.navTick();
      this.scope.set(nextScope);
      this.page.set(0);
      this.loadSupervision();
    });
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openMenuId.set(null);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.openMenuId.set(null);
    this.closeDetail();
  }

  loadSupervision(): void {
    this.loading.set(true);
    this.error.set('');
    this.messagerieService
      .getAdminSupervision(this.scope(), this.query(), this.page(), this.size)
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res.stats) this.stats.set(res.stats);
          this.rows.set(res.conversations ?? []);
          this.totalElements.set(res.totalElements ?? res.conversations?.length ?? 0);
          this.totalPages.set(res.totalPages ?? (res.conversations?.length ? 1 : 0));
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(this.readError(err));
        }
      });
  }

  setScope(scope: SupervisionScope): void {
    this.scope.set(scope);
    this.page.set(0);
    this.loadSupervision();
  }

  onSearchChange(value: string): void {
    this.query.set(value);
    this.page.set(0);
    this.loadSupervision();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.page()) return;
    this.page.set(page);
    this.loadSupervision();
  }

  toggleMenu(id: string, event: Event): void {
    event.stopPropagation();
    this.openMenuId.update((current) => (current === id ? null : id));
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
          setTimeout(() => {
            document.getElementById('ma-signalements')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          });
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
    const archivee = !row.archivee;
    this.detailBusy.set(true);
    this.messagerieService.updateAdminConversationModeration(row.id, { archivee }).subscribe({
      next: (updated) => {
        this.detailBusy.set(false);
        this.toast.emit(archivee ? 'Conversation archivée.' : 'Conversation désarchivée.');
        this.loadSupervision();
        if (this.selectedDetail()?.conversation.id === updated.id) {
          this.openDetail(updated);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
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
        const updatedSigs = (current.signalements ?? []).map((item) =>
          item.id === revealed.id ? revealed : item
        );
        this.selectedDetail.set({ ...current, signalements: updatedSigs });
        this.messagerieService.getAdminConversationDetail(current.conversation.id).subscribe({
          next: (fresh) => {
            this.selectedDetail.set({
              ...fresh,
              signalements: (fresh.signalements ?? []).map((item) =>
                item.id === revealed.id ? revealed : item
              )
            });
          },
          error: () => {}
        });
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
        this.toast.emit(this.readError(err));
      }
    });
  }

  setSignalementAction(sigId: string, action: string): void {
    this.signalementActionMap.update((map) => ({
      ...map,
      [sigId]: action as SignalementActionAdmin
    }));
  }

  setSignalementNote(sigId: string, note: string): void {
    this.signalementNoteMap.update((map) => ({ ...map, [sigId]: note }));
  }

  traiterSignalement(sig: SignalementItem): void {
    const action = this.signalementActionMap()[sig.id] ?? 'IGNORER';
    const note = this.signalementNoteMap()[sig.id] ?? '';
    this.detailBusy.set(true);

    this.messagerieService.traiterSignalement(sig.id, action, note).subscribe({
      next: () => {
        this.detailBusy.set(false);
        this.toast.emit('Signalement traité.');
        this.loadSupervision();
        const detail = this.selectedDetail();
        if (detail) {
          this.openDetail(detail.conversation);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.detailBusy.set(false);
        this.toast.emit(this.readError(err));
      }
    });
  }

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
        const detail = this.selectedDetail();
        if (detail && detail.conversation.id === convId) {
          this.openDetail(detail.conversation);
        }
      },
      error: (err: HttpErrorResponse) => this.toast.emit(this.readError(err))
    });
  }

  formatDateTime(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  roleLabel(role: string | null | undefined): string {
    return formaterRoleLibelle(role);
  }

  statutTone(statut: string | null | undefined): string {
    const norm = String(statut ?? '').toUpperCase();
    if (norm === 'SIGNALEE' || norm === 'EN_ATTENTE' || norm === 'ENVOI_SUSPENDU') return 'red';
    if (norm === 'ARCHIVEE' || norm === 'IGNORE') return 'gray';
    if (norm === 'AVERTI' || norm === 'MESSAGE_MASQUE') return 'amber';
    return 'green';
  }

  statutLabel(statut: string | null | undefined): string {
    switch (String(statut ?? '').toUpperCase()) {
      case 'SIGNALEE':
        return 'Signalée';
      case 'ARCHIVEE':
        return 'Archivée';
      case 'ACTIVE':
        return 'Active';
      case 'EN_ATTENTE':
        return 'En attente';
      case 'IGNORE':
        return 'Ignoré';
      case 'AVERTI':
        return 'Averti';
      case 'MESSAGE_MASQUE':
        return 'Message masqué';
      case 'ENVOI_SUSPENDU':
        return 'Envoi suspendu';
      default:
        return statut || 'Active';
    }
  }

  private initSignalementControls(items: SignalementItem[]): void {
    const actions: Record<string, SignalementActionAdmin> = {};
    const notes: Record<string, string> = {};
    for (const s of items) {
      actions[s.id] = (s.actionAdmin as SignalementActionAdmin) || 'IGNORER';
      notes[s.id] = '';
    }
    this.signalementActionMap.set(actions);
    this.signalementNoteMap.set(notes);
  }

  private readError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Impossible de joindre le serveur.';
    }
    return err.error?.message ?? err.error?.detail ?? 'Service messagerie indisponible pour le moment.';
  }

  private downloadCsvBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }
}
