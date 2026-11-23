import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AuthService } from '../user/auth.service';
import { UserService } from '../user/user.service';
import { User } from '../user/user.model';
import { Offre } from '../offres/offre.model';
import { OffreService } from '../offres/offre.service';
import { Candidature, CandidatureCreation, CandidatureStatut, CandidatureUpdate } from './candidature.model';

@Component({
  selector: 'app-candidature-form', standalone: true, imports: [FormsModule],
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
  lettre = '';
  cvId: number | null = null;
  userId: number | null = null;
  statut: CandidatureStatut = 'RECUE';
  entretien = '';
  note: number | null = null;
  commentaire = '';

  ngOnInit(): void {
    const c = this.candidature;
    if (c) { // Copie les valeurs sans modifier la carte affichée.
      this.lettre = c.lettre; this.cvId = c.cvId; this.statut = c.statut;
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

  enregistrer(): void {
    if (!this.candidature) {
      if (!this.offreId || !this.cvId) return;
      this.valider.emit({ offreId: this.offreId, cvId: this.cvId, lettre: this.lettre.trim(), ...(this.role === 'ADMIN' && this.userId ? { userId: this.userId } : {}) });
      return;
    }
    const demande: CandidatureUpdate = {};
    if (this.role !== 'RECRUTEUR') { demande.lettre = this.lettre.trim(); demande.cvId = this.cvId!; }
    if (this.role !== 'CANDIDAT') {
      if (this.statut !== this.candidature.statut) demande.statut = this.statut;
      // N'envoie pas une ancienne date inchangée : le backend valide les nouvelles dates futures.
      if (this.entretien && this.entretien !== (this.candidature.dateEntretien?.slice(0, 16) || '')) demande.dateEntretien = this.entretien + ':00';
      if (this.note !== null) demande.noteRecruteur = this.note;
      demande.commentaireRecruteur = this.commentaire;
    }
    this.valider.emit(demande);
  }
}
