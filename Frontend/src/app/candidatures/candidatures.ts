import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../user/auth.service';
import { Candidature, CandidatureCreation, CandidatureUpdate } from './candidature.model';
import { CandidatureService } from './candidature.service';
import { CandidatureForm } from './candidature-form';

@Component({
  selector: 'app-candidatures', standalone: true, imports: [CommonModule, FormsModule, CandidatureForm],
  templateUrl: './candidatures.html', styleUrls: ['../app.css', '../offres/offres.css', './candidatures.css']
})
export class Candidatures implements OnDestroy {
  readonly refresh = input(0);
  readonly auth = inject(AuthService);
  private readonly api = inject(CandidatureService);
  private requete?: Subscription;
  readonly liste = signal<Candidature[]>([]);
  readonly loading = signal(false);
  readonly operation = signal(false);
  readonly erreur = signal('');
  readonly message = signal('');
  readonly formulaire = signal(false);
  readonly edition = signal<Candidature | null>(null);
  readonly suppression = signal<Candidature | null>(null);
  readonly erreurFormulaire = signal('');
  search = '';
  sort = 'dateCandidatureDesc';

  constructor() {
    effect(() => {
      this.auth.currentUser(); this.refresh();
      untracked(() => { this.formulaire.set(false); this.suppression.set(null); this.message.set(''); this.charger(); });
    });
  }

  charger(): void {
    this.requete?.unsubscribe(); // Annule la liste de l'ancienne session.
    this.liste.set([]); this.erreur.set('');
    this.loading.set(this.auth.isLoggedIn());
    if (!this.auth.isLoggedIn()) return;
    this.requete = this.api.lister(this.search.trim(), this.sort).subscribe({
      next: liste => { this.liste.set(liste); this.loading.set(false); },
      error: erreur => { this.erreur.set(erreur.error?.message || 'Impossible de charger les candidatures. Reconnectez-vous ou réessayez.'); this.loading.set(false); }
    });
  }

  ouvrir(c: Candidature | null = null): void {
    this.edition.set(c); this.erreurFormulaire.set(''); this.formulaire.set(true);
  }

  enregistrer(demande: CandidatureCreation | CandidatureUpdate): void {
    const c = this.edition();
    const requete = 'offreId' in demande ? this.api.creer(demande) : c ? this.api.modifier(c.id, demande) : null;
    if (!requete || this.operation()) return;
    this.operation.set(true); this.erreurFormulaire.set('');
    requete.subscribe({
      next: () => { this.operation.set(false); this.formulaire.set(false); this.message.set('Candidature enregistrée.'); this.charger(); },
      error: erreur => { this.operation.set(false); this.erreurFormulaire.set(erreur.error?.message || 'Enregistrement impossible. Vérifiez votre connexion.'); }
    });
  }

  supprimer(): void {
    const c = this.suppression();
    if (!c || this.operation()) return;
    this.operation.set(true);
    this.api.supprimer(c.id).subscribe({
      next: () => { this.operation.set(false); this.suppression.set(null); this.message.set(this.auth.isAdmin() ? 'Candidature supprimée.' : 'Candidature retirée.'); this.charger(); },
      error: erreur => { this.operation.set(false); this.suppression.set(null); this.erreur.set(erreur.error?.message || 'Opération impossible. Réessayez.'); }
    });
  }

  modifiable(c: Candidature): boolean { // Le backend vérifie aussi ces droits.
    const role = this.auth.currentUser()?.role;
    return role === 'ADMIN' || (role === 'CANDIDAT' ? c.statut === 'RECUE' : c.statut !== 'RETIREE');
  }

  @HostListener('document:keydown.escape')
  fermer(): void {
    if (!this.operation()) { this.formulaire.set(false); this.suppression.set(null); }
  }

  ngOnDestroy(): void { this.requete?.unsubscribe(); }
}
