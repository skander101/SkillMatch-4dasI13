import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { User } from '../user/user.model';
import { Alerte, Favori, Frequence } from './alertes.model';
import { AlertesService, alertesErrorMessage } from './alertes.service';

export type AlertesBackofficeView = 'alertes' | 'favoris';

/** Back office « Alertes & Favoris » : données réelles via GET /skillmatch/alerts/admin et /skillmatch/favorites/admin. */
@Component({
  selector: 'app-alertes-backoffice',
  standalone: true,
  templateUrl: './backoffice-alertes.html',
  styleUrl: './backoffice-alertes.css'
})
export class BackofficeAlertes {
  private readonly api = inject(AlertesService);

  readonly view = input.required<AlertesBackofficeView>();
  /** Utilisateurs déjà chargés par le back office (commun-service), pour afficher le nom du candidat. */
  readonly utilisateurs = input<User[]>([]);

  readonly alertes = signal<Alerte[]>([]);
  readonly favoris = signal<Favori[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly feedback = signal('');
  readonly filtre = signal('');
  readonly aSupprimer = signal<number | null>(null);

  readonly nbActives = computed(() => this.alertes().filter((alerte) => alerte.active).length);
  readonly nbCandidats = computed(() => new Set([
    ...this.alertes().map((alerte) => alerte.userId),
    ...this.favoris().map((favori) => favori.userId)
  ]).size);

  /** Offres les plus sauvegardées (top 5). */
  readonly topOffres = computed(() => {
    const compteur = new Map<number, number>();
    for (const favori of this.favoris()) {
      compteur.set(favori.offerId, (compteur.get(favori.offerId) ?? 0) + 1);
    }
    return [...compteur.entries()]
      .map(([offerId, total]) => ({ offerId, total }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);
  });

  readonly alertesFiltrees = computed(() => {
    const q = this.filtre().trim().toLowerCase();
    if (!q) return this.alertes();
    return this.alertes().filter((alerte) => [
      this.candidat(alerte.userId),
      ...alerte.motsCles,
      alerte.localisation ?? '',
      alerte.contrat ?? '',
      alerte.frequence
    ].some((valeur) => valeur.toLowerCase().includes(q)));
  });

  readonly favorisFiltres = computed(() => {
    const q = this.filtre().trim().toLowerCase();
    if (!q) return this.favoris();
    return this.favoris().filter((favori) =>
      [this.candidat(favori.userId), String(favori.offerId)].some((valeur) => valeur.toLowerCase().includes(q)));
  });

  private feedbackTimer?: number;

  constructor() {
    // Recharge quand on passe de « Alertes » à « Favoris » dans le menu latéral.
    effect(() => {
      this.view();
      untracked(() => {
        this.filtre.set('');
        this.aSupprimer.set(null);
        this.charger();
      });
    });
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    let restants = 2;
    const fin = () => { if (--restants === 0) this.chargement.set(false); };
    this.api.toutesLesAlertes().subscribe({
      next: (alertes) => { this.alertes.set(alertes); fin(); },
      error: (error) => { this.erreur.set(alertesErrorMessage(error, 'Impossible de charger les alertes.')); fin(); }
    });
    this.api.tousLesFavoris().subscribe({
      next: (favoris) => { this.favoris.set(favoris); fin(); },
      error: (error) => { this.erreur.set(alertesErrorMessage(error, 'Impossible de charger les favoris.')); fin(); }
    });
  }

  basculerActive(alerte: Alerte): void {
    this.erreur.set('');
    this.api.adminChangerActive(alerte.id, !alerte.active).subscribe({
      next: (maj) => {
        this.alertes.update((liste) => liste.map((item) => item.id === maj.id ? maj : item));
        this.afficherFeedback(maj.active ? `Alerte n° ${maj.id} activée.` : `Alerte n° ${maj.id} désactivée.`);
      },
      error: (error) => this.erreur.set(alertesErrorMessage(error, 'Changement de statut impossible.'))
    });
  }

  supprimerAlerte(id: number): void {
    this.erreur.set('');
    this.api.adminSupprimerAlerte(id).subscribe({
      next: () => {
        this.alertes.update((liste) => liste.filter((item) => item.id !== id));
        this.aSupprimer.set(null);
        this.afficherFeedback(`Alerte n° ${id} supprimée.`);
      },
      error: (error) => {
        this.aSupprimer.set(null);
        this.erreur.set(alertesErrorMessage(error, 'Suppression impossible.'));
      }
    });
  }

  supprimerFavori(id: number): void {
    this.erreur.set('');
    this.api.adminSupprimerFavori(id).subscribe({
      next: () => {
        this.favoris.update((liste) => liste.filter((item) => item.id !== id));
        this.aSupprimer.set(null);
        this.afficherFeedback(`Favori n° ${id} supprimé.`);
      },
      error: (error) => {
        this.aSupprimer.set(null);
        this.erreur.set(alertesErrorMessage(error, 'Suppression impossible.'));
      }
    });
  }

  candidat(userId: number): string {
    const user = this.utilisateurs().find((item) => item.id === userId);
    return user ? user.nom : `Utilisateur n° ${userId}`;
  }

  email(userId: number): string {
    return this.utilisateurs().find((item) => item.id === userId)?.email ?? '';
  }

  libelleFrequence(value: Frequence): string {
    return { IMMEDIATE: 'Immédiate', QUOTIDIENNE: 'Quotidienne', HEBDOMADAIRE: 'Hebdomadaire' }[value] ?? value;
  }

  dateCourte(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('fr-FR');
  }

  private afficherFeedback(message: string): void {
    this.feedback.set(message);
    window.clearTimeout(this.feedbackTimer);
    this.feedbackTimer = window.setTimeout(() => this.feedback.set(''), 4000);
  }
}
