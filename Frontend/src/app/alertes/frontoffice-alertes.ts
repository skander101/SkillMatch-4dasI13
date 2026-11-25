import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { AuthService } from '../user/auth.service';
import { Alerte, AlerteRequest, ContratType, Favori, Frequence } from './alertes.model';
import { AlertesService, alertesErrorMessage } from './alertes.service';

export type AlertesView = 'alertes' | 'favoris';

/** Résumé d'une offre affichée sur la page publique (pour afficher le titre d'un favori). */
export interface OffreResume {
  id: number;
  title: string;
  company: string;
  location: string;
  contract: string;
}

/** Front office du microservice Alertes & Favoris : pages « Mes alertes » et « Mes favoris ». */
@Component({
  selector: 'app-alertes-frontoffice',
  standalone: true,
  templateUrl: './frontoffice-alertes.html',
  styleUrl: './frontoffice-alertes.css'
})
export class FrontofficeAlertes {
  private readonly api = inject(AlertesService);
  private readonly auth = inject(AuthService);

  readonly vue = input.required<AlertesView>();
  readonly offres = input<OffreResume[]>([]);
  readonly vueChange = output<AlertesView>();
  readonly connexion = output<void>();
  readonly voirOffres = output<void>();

  readonly maxActives = 5;
  readonly contrats: ContratType[] = ['CDI', 'CDD', 'STAGE', 'FREELANCE'];
  readonly frequences: { value: Frequence; label: string }[] = [
    { value: 'IMMEDIATE', label: 'Immédiate' },
    { value: 'QUOTIDIENNE', label: 'Quotidienne' },
    { value: 'HEBDOMADAIRE', label: 'Hebdomadaire' }
  ];
  readonly villes = ['Tunis', 'Sfax', 'Sousse', 'Ariana', 'Télétravail'];

  readonly estConnecte = this.auth.isLoggedIn;
  readonly estCandidat = computed(() => this.auth.isLoggedIn() && this.auth.currentUser()?.role === 'CANDIDAT');

  readonly alertes = signal<Alerte[]>([]);
  readonly favoris = this.api.favoris;
  readonly chargement = signal(false);
  readonly enregistrement = signal(false);
  readonly erreur = signal('');
  readonly feedback = signal('');
  readonly aSupprimer = signal<number | null>(null);

  // Formulaire (création ou modification)
  readonly editionId = signal<number | null>(null);
  readonly motsCles = signal('');
  readonly localisation = signal('');
  readonly contrat = signal<ContratType | ''>('');
  readonly frequence = signal<Frequence>('IMMEDIATE');

  readonly nbActives = computed(() => this.alertes().filter((alerte) => alerte.active).length);
  readonly quotaAtteint = computed(() => this.nbActives() >= this.maxActives);

  private feedbackTimer?: number;

  constructor() {
    // Charge les données dès qu'un candidat est connecté (y compris après une connexion depuis cette page).
    effect(() => {
      if (this.estCandidat()) {
        untracked(() => this.charger());
      } else {
        untracked(() => this.alertes.set([]));
      }
    });
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set('');
    this.api.mesAlertes().subscribe({
      next: (alertes) => {
        this.alertes.set(alertes);
        this.chargement.set(false);
      },
      error: (error) => {
        this.chargement.set(false);
        this.erreur.set(alertesErrorMessage(error, 'Impossible de charger vos alertes.'));
      }
    });
    this.api.chargerFavoris().subscribe({
      error: (error) => this.erreur.set(alertesErrorMessage(error, 'Impossible de charger vos favoris.'))
    });
  }

  changerVue(vue: AlertesView): void {
    this.erreur.set('');
    this.aSupprimer.set(null);
    this.vueChange.emit(vue);
  }

  // ---------- Alertes ----------

  enregistrer(): void {
    const motsCles = this.motsCles().split(',').map((mot) => mot.trim()).filter((mot) => mot.length > 0);
    if (!motsCles.length) {
      this.erreur.set('Ajoutez au moins un mot-clé (séparez-les par des virgules).');
      return;
    }
    const request: AlerteRequest = {
      motsCles,
      localisation: this.localisation().trim() || null,
      contrat: this.contrat() || null,
      frequence: this.frequence()
    };
    const id = this.editionId();
    this.enregistrement.set(true);
    this.erreur.set('');
    const appel = id === null ? this.api.creerAlerte(request) : this.api.modifierAlerte(id, request);
    appel.subscribe({
      next: (alerte) => {
        this.enregistrement.set(false);
        this.alertes.update((liste) => id === null
          ? [alerte, ...liste]
          : liste.map((item) => item.id === alerte.id ? alerte : item));
        this.annulerEdition();
        this.afficherFeedback(id === null ? 'Alerte créée. Vous serez prévenu des nouvelles offres correspondantes.' : 'Alerte modifiée.');
      },
      error: (error) => {
        this.enregistrement.set(false);
        this.erreur.set(alertesErrorMessage(error, 'Enregistrement impossible.'));
      }
    });
  }

  modifier(alerte: Alerte): void {
    this.editionId.set(alerte.id);
    this.motsCles.set(alerte.motsCles.join(', '));
    this.localisation.set(alerte.localisation ?? '');
    this.contrat.set(alerte.contrat ?? '');
    this.frequence.set(alerte.frequence);
    this.erreur.set('');
  }

  annulerEdition(): void {
    this.editionId.set(null);
    this.motsCles.set('');
    this.localisation.set('');
    this.contrat.set('');
    this.frequence.set('IMMEDIATE');
  }

  basculerActive(alerte: Alerte): void {
    this.erreur.set('');
    this.api.changerActive(alerte.id, !alerte.active).subscribe({
      next: (maj) => {
        this.alertes.update((liste) => liste.map((item) => item.id === maj.id ? maj : item));
        this.afficherFeedback(maj.active ? 'Alerte activée.' : 'Alerte mise en pause.');
      },
      error: (error) => this.erreur.set(alertesErrorMessage(error, 'Changement de statut impossible.'))
    });
  }

  supprimer(id: number): void {
    this.erreur.set('');
    this.api.supprimerAlerte(id).subscribe({
      next: () => {
        this.alertes.update((liste) => liste.filter((item) => item.id !== id));
        this.aSupprimer.set(null);
        if (this.editionId() === id) {
          this.annulerEdition();
        }
        this.afficherFeedback('Alerte supprimée.');
      },
      error: (error) => {
        this.aSupprimer.set(null);
        this.erreur.set(alertesErrorMessage(error, 'Suppression impossible.'));
      }
    });
  }

  // ---------- Favoris ----------

  offre(offerId: number): OffreResume | undefined {
    return this.offres().find((offre) => offre.id === offerId);
  }

  retirerFavori(favori: Favori): void {
    this.erreur.set('');
    this.api.retirerFavoriEtActualiser(favori.id).subscribe({
      next: () => this.afficherFeedback('Offre retirée de vos favoris.'),
      error: (error) => this.erreur.set(alertesErrorMessage(error, 'Impossible de retirer ce favori.'))
    });
  }

  // ---------- Affichage ----------

  libelleFrequence(value: Frequence): string {
    return this.frequences.find((item) => item.value === value)?.label ?? value;
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
