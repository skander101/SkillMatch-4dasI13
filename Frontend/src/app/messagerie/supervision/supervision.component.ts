import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdminConversationDetail,
  AdminConversationSummary,
  MessagerieAdminStats,
  SupervisionScope
} from '../messagerie.models';
import { MessagerieService } from '../messagerie.service';

@Component({
  selector: 'app-messagerie-supervision',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supervision.component.html',
  styleUrl: './supervision.component.css'
})
export class SupervisionComponent implements OnInit, OnDestroy {
  private readonly api = inject(MessagerieService);
  private searchTimer?: number;

  readonly scope = signal<SupervisionScope>('TOUTES');
  readonly query = signal('');
  readonly loading = signal(false);
  readonly exporting = signal(false);
  readonly error = signal('');
  readonly toast = signal('');

  readonly stats = signal<MessagerieAdminStats>({
    conversations: 0,
    groupes: 0,
    messagesAujourdhui: 0,
    signalementsATraiter: 0,
    nouvellesConversationsSemaine: 0,
    nouveauxGroupesSemaine: 0,
    heurePicActivite: null,
    heurePicLibelle: null
  });
  readonly conversations = signal<AdminConversationSummary[]>([]);

  readonly detailOpen = signal(false);
  readonly detailLoading = signal(false);
  readonly detailError = signal('');
  readonly detail = signal<AdminConversationDetail | null>(null);
  readonly moderating = signal(false);

  readonly scopes: ReadonlyArray<{ key: SupervisionScope; label: string }> = [
    { key: 'TOUTES', label: 'Toutes' },
    { key: 'GROUPES', label: 'Groupes' },
    { key: 'ARCHIVEES', label: 'Archivées' }
  ];

  readonly totalDisplayed = computed(() => this.conversations().length);

  ngOnInit(): void {
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    if (this.searchTimer) {
      window.clearTimeout(this.searchTimer);
    }
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getAdminSupervision(this.scope(), this.query()).subscribe({
      next: (res) => {
        this.stats.set(res.stats);
        this.conversations.set(res.conversations ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Impossible de charger les données de supervision.');
      }
    });
  }

  selectScope(next: SupervisionScope): void {
    if (this.scope() === next) return;
    this.scope.set(next);
    this.loadDashboard();
  }

  onQueryChange(value: string): void {
    this.query.set(value);
    if (this.searchTimer) {
      window.clearTimeout(this.searchTimer);
    }
    this.searchTimer = window.setTimeout(() => {
      this.loadDashboard();
    }, 250);
  }

  clearQuery(): void {
    if (!this.query()) return;
    this.query.set('');
    this.loadDashboard();
  }

  exportCsv(): void {
    this.exporting.set(true);
    this.error.set('');
    this.api.exportAdminSupervisionCsv(this.scope(), this.query()).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'messagerie-supervision.csv';
        link.click();
        window.URL.revokeObjectURL(url);
        this.showToast('Export CSV téléchargé.');
      },
      error: (err) => {
        this.exporting.set(false);
        this.error.set(err?.error?.message || "Erreur lors de l'export CSV.");
      }
    });
  }

  openDetail(row: AdminConversationSummary): void {
    this.detailOpen.set(true);
    this.detailLoading.set(true);
    this.detailError.set('');
    this.detail.set(null);
    this.api.getAdminConversationDetail(row.id).subscribe({
      next: (res) => {
        this.detail.set(res);
        this.detailLoading.set(false);
      },
      error: (err) => {
        this.detailLoading.set(false);
        this.detailError.set(err?.error?.message || 'Impossible de charger les détails de la conversation.');
      }
    });
  }

  closeDetail(): void {
    this.detailOpen.set(false);
    this.detail.set(null);
    this.detailError.set('');
  }

  toggleFlag(conv: AdminConversationSummary): void {
    this.moderating.set(true);
    this.api.updateAdminConversationModeration(conv.id, { signalee: !conv.signalee }).subscribe({
      next: (updated) => {
        this.moderating.set(false);
        const currentDetail = this.detail();
        if (currentDetail && currentDetail.conversation.id === updated.id) {
          this.detail.set({ ...currentDetail, conversation: updated });
        }
        this.showToast(updated.signalee ? 'Conversation marquée comme signalée.' : 'Signalement retiré.');
        this.loadDashboard();
      },
      error: (err) => {
        this.moderating.set(false);
        this.detailError.set(err?.error?.message || 'Impossible de mettre à jour le signalement.');
      }
    });
  }

  toggleArchive(conv: AdminConversationSummary): void {
    this.moderating.set(true);
    this.api.updateAdminConversationModeration(conv.id, { archivee: !conv.archivee }).subscribe({
      next: (updated) => {
        this.moderating.set(false);
        const currentDetail = this.detail();
        if (currentDetail && currentDetail.conversation.id === updated.id) {
          this.detail.set({ ...currentDetail, conversation: updated });
        }
        this.showToast(updated.archivee ? 'Conversation archivée.' : 'Conversation désarchivée.');
        this.loadDashboard();
      },
      error: (err) => {
        this.moderating.set(false);
        this.detailError.set(err?.error?.message || "Impossible de mettre à jour l'archivage.");
      }
    });
  }

  formatTypeLabel(type: string): string {
    return type?.toLowerCase() === 'groupe' ? 'Groupe' : 'Directe';
  }

  formatStatutLabel(statut: string): string {
    switch (statut?.toLowerCase()) {
      case 'archivee':
        return 'Archivée';
      default:
        return 'Active';
    }
  }

  statutClass(statut: string): string {
    switch (statut?.toLowerCase()) {
      case 'archivee':
        return 'gray';
      default:
        return 'green';
    }
  }

  formatRelativeDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '—';
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return "À l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays === 1) return 'Hier';
    if (diffDays < 7) return `Il y a ${diffDays} jours`;
    return date.toLocaleDateString('fr-FR');
  }

  formatExactDate(iso: string | null | undefined): string {
    if (!iso) return '';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private showToast(message: string): void {
    this.toast.set(message);
    window.setTimeout(() => {
      if (this.toast() === message) {
        this.toast.set('');
      }
    }, 3500);
  }
}
