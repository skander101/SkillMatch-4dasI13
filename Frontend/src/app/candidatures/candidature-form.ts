import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../user/auth.service';
import { UserService } from '../user/user.service';
import { User } from '../user/user.model';
import { Offre } from '../offres/offre.model';
import { OffreService } from '../offres/offre.service';
import { Candidature, CandidatureCreation, CandidatureStatut, CandidatureUpdate } from './candidature.model';
import { SaisieValide } from '../saisie-valide';

@Component({
  selector: 'app-candidature-form', standalone: true, imports: [FormsModule, SaisieValide],
  templateUrl: './candidature-form.html', styleUrls: ['../app.css', '../offres/offres.css']
})
export class CandidatureForm implements OnInit {
  @Input() candidature: Candidature | null = null;
  @Input() offreId: number | null = null;
  @Input() enCours = false;
  @Input() erreur = '';
  @Output() valider = new EventEmitter<CandidatureCreation | CandidatureUpdate>();
  @Output() annuler = new EventEmitter<void>();
  readonly role = inject(AuthService).currentUser()?.role;
  private readonly utilisateurs = inject(UserService);
  private readonly apiOffres = inject(OffreService);
  readonly offres = signal<Offre[]>([]);
  readonly candidats = signal<User[]>([]);
  readonly chargement = signal(false);
  readonly erreurChoix = signal('');
  readonly statuts: CandidatureStatut[] = this.role === 'ADMIN'
    ? ['RECUE', 'PRESELECTIONNEE', 'ENTRETIEN_PLANIFIE', 'ACCEPTEE', 'REFUSEE', 'RETIREE']
    : ['PRESELECTIONNEE', 'ENTRETIEN_PLANIFIE', 'ACCEPTEE', 'REFUSEE'];
  readonly aujourdHui = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  lettre = '';
  userId: number | null = null;
  statut: CandidatureStatut = 'RECUE';
  entretien = '';
  note: number | null = null;
  commentaire = '';

  ngOnInit(): void {
    const c = this.candidature;
    if (c) { // Copie les valeurs sans modifier la carte affichée.
      this.lettre = c.lettre; this.statut = c.statut;
      this.entretien = c.dateEntretien?.slice(0, 16) || '';
      this.note = c.noteRecruteur; this.commentaire = c.commentaireRecruteur || '';
    }
    if (this.role !== 'ADMIN' || c) return;
    this.chargement.set(true);
    forkJoin({ offres: this.apiOffres.lister('', 'titre'), users: this.utilisateurs.list() }).subscribe({
      next: ({ offres, users }) => {
        const maintenant = new Date();
        const aujourdHui = new Date(maintenant.getTime() - maintenant.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
        this.offres.set(offres.filter(o => o.statut === 'PUBLIEE' && o.dateExpiration >= aujourdHui));
        this.candidats.set(users.filter(u => u.role === 'CANDIDAT'));
        this.chargement.set(false);
      },
      error: () => { this.erreurChoix.set('Impossible de charger les offres et candidats. Fermez puis réessayez.'); this.chargement.set(false); }
    });
  }

  erreurEntretien(): string {
    if (!this.candidature || this.role === 'CANDIDAT') return '';
    if (!this.entretien) return 'Choisissez une date et une heure pour l’entretien.';
    if (!Number.isFinite(Date.parse(this.entretien)) || this.entretien.slice(0, 10) < this.aujourdHui) {
      return 'La date d’entretien doit être aujourd’hui ou dans le futur.';
    }
    return '';
  }

  enregistrer(valide = true): void {
    if (!valide || this.enCours || this.chargement() || this.erreurChoix() || this.erreurEntretien()) return;
    if (this.role !== 'RECRUTEUR' && !this.lettre.trim()) return;
    if (!this.candidature) {
      if (!this.offreId) return;
      this.valider.emit({ offreId: this.offreId, lettre: this.lettre.trim(), ...(this.role === 'ADMIN' && this.userId ? { userId: this.userId } : {}) });
      return;
    }
    const demande: CandidatureUpdate = {};
    if (this.role !== 'RECRUTEUR') { demande.lettre = this.lettre.trim(); }
    if (this.role !== 'CANDIDAT') {
      if (this.statut !== this.candidature.statut) demande.statut = this.statut;
      // Envoie les champs validés, y compris pour ADMIN.
      demande.dateEntretien = this.entretien.length === 16 ? this.entretien + ':00' : this.entretien;
      if (this.note !== null) demande.noteRecruteur = this.note;
      demande.commentaireRecruteur = this.commentaire.trim();
    }
    this.valider.emit(demande);
  }
}
