import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, HostListener, OnDestroy, Output, computed, effect, inject, input, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../user/auth.service';
import { Offre, OffreRequest, OffreStatut } from './offre.model';
import { OffreService } from './offre.service';
import { OffreForm } from './offre-form';
import { CandidatureForm } from '../candidatures/candidature-form';
import { CandidatureService } from '../candidatures/candidature.service';
import { CandidatureCreation, CandidatureUpdate } from '../candidatures/candidature.model';

@Component({
  selector: 'app-offres',
  standalone: true,
  imports: [CommonModule, FormsModule, OffreForm, CandidatureForm],
  templateUrl: './offres.html',
  styleUrls: ['../app.css', './offres.css']
})
export class Offres implements OnDestroy {
  readonly search = input('');
  readonly sort = input('titre');
  @Output() connexion = new EventEmitter<void>();
  @Output() candidatureEnvoyee = new EventEmitter<void>();
  readonly auth = inject(AuthService);
  private readonly api = inject(OffreService);
  private readonly candidatures = inject(CandidatureService);
  private chargement?: Subscription;
  readonly recruteur = computed(() => this.auth.currentUser()?.role === 'RECRUTEUR');
  readonly gestion = computed(() => this.recruteur() || this.auth.isAdmin());
  readonly postulation = signal<Offre | null>(null);
  readonly erreurPostulation = signal('');
  rechercheAdmin = '';
  triAdmin = 'titre';
  readonly offres = signal<Offre[]>([]);
  readonly loading = signal(false);
  readonly operation = signal(false);
  readonly erreur = signal('');
  readonly codeErreur = signal(0);
  readonly message = signal('');
  readonly selection = signal<Offre | null>(null);
  readonly edition = signal<Offre | null>(null);
  readonly formulaireOuvert = signal(false);
  readonly suppression = signal<Offre | null>(null);
  readonly erreurFormulaire = signal('');

  constructor() {
    effect(() => { // Recharge après connexion, changement de recherche ou de tri.
      this.auth.currentUser();
      this.formulaireOuvert.set(false);
      this.selection.set(null);
      this.suppression.set(null);
      this.postulation.set(null);
      this.message.set('');
      this.charger();
    });
  }

  charger(): void {
    this.chargement?.unsubscribe(); // Évite qu'une ancienne session remplace les nouveaux résultats.
    this.offres.set([]);
    this.erreur.set('');
    this.codeErreur.set(0);
    this.loading.set(this.auth.isLoggedIn());
    if (!this.auth.isLoggedIn()) return;
    this.chargement = this.api.lister(this.auth.isAdmin() ? this.rechercheAdmin : this.search(), this.auth.isAdmin() ? this.triAdmin : this.sort(), this.recruteur()).subscribe({
      next: offres => { this.offres.set(offres); this.loading.set(false); },
      error: erreur => { this.erreur.set(this.lireErreur(erreur)); this.codeErreur.set(erreur.status); this.loading.set(false); }
    });
  }

  ouvrirCandidature(offre: Offre): void {
    this.selection.set(null); this.erreurPostulation.set(''); this.postulation.set(offre);
  }

  postuler(demande: CandidatureCreation | CandidatureUpdate): void {
    if (!('offreId' in demande) || this.operation()) return;
    this.operation.set(true); this.erreurPostulation.set('');
    this.candidatures.creer(demande).subscribe({
      next: () => { this.operation.set(false); this.postulation.set(null); this.message.set('Candidature envoyée. Retrouvez-la dans Mes candidatures.'); this.candidatureEnvoyee.emit(); },
      error: erreur => { this.operation.set(false); this.erreurPostulation.set(this.lireErreur(erreur)); }
    });
  }

  ouvrirFormulaire(offre: Offre | null = null): void {
    this.edition.set(offre);
    this.erreurFormulaire.set('');
    this.formulaireOuvert.set(true);
  }

  fermerFormulaire(): void {
    if (!this.operation()) this.formulaireOuvert.set(false);
  }

  enregistrer(demande: OffreRequest): void {
    this.operation.set(true);
    this.erreurFormulaire.set('');
    const offre = this.edition();
    const requete = offre ? this.api.modifier(offre.id, demande) : this.api.creer(demande);
    requete.subscribe({
      next: () => { this.operation.set(false); this.formulaireOuvert.set(false); this.message.set('Offre enregistrée.'); this.charger(); },
      error: erreur => { this.operation.set(false); this.erreurFormulaire.set(this.lireErreur(erreur)); }
    });
  }

  changerStatut(offre: Offre, statut: OffreStatut): void {
    this.operation.set(true);
    this.api.modifier(offre.id, { ...offre, statut }).subscribe({
      next: () => { this.operation.set(false); this.message.set(statut === 'PUBLIEE' ? 'Offre publiée.' : 'Offre fermée.'); this.charger(); },
      error: erreur => { this.operation.set(false); this.erreur.set(this.lireErreur(erreur)); }
    });
  }

  supprimer(): void {
    const offre = this.suppression();
    if (!offre || this.operation()) return;
    this.operation.set(true);
    this.api.supprimer(offre.id).subscribe({
      next: () => { this.operation.set(false); this.suppression.set(null); this.message.set('Offre supprimée.'); this.charger(); },
      error: erreur => { this.operation.set(false); this.suppression.set(null); this.erreur.set(this.lireErreur(erreur)); }
    });
  }

  private lireErreur(erreur: HttpErrorResponse): string {
    return erreur.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' : erreur.error?.message || 'Impossible de joindre le service. Réessayez dans un instant.';
  }

  @HostListener('document:keydown.escape')
  fermer(): void {
    this.fermerFormulaire();
    this.selection.set(null);
    if (!this.operation()) { this.suppression.set(null); this.postulation.set(null); }
  }

  ngOnDestroy(): void {
    this.chargement?.unsubscribe();
  }
}
