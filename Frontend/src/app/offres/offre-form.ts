import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Offre, OffreRequest, OffreStatut } from './offre.model';
import { UserService } from '../user/user.service';
import { User } from '../user/user.model';
import { forkJoin, of } from 'rxjs';
import { ReferencesService, ChoixReference } from './references.service';
import { SaisieValide } from '../saisie-valide';

@Component({
  selector: 'app-offre-form',
  standalone: true,
  imports: [FormsModule, SaisieValide],
  templateUrl: './offre-form.html',
  styleUrls: ['../app.css', './offres.css'] // Réutilise le thème et les modales du template.
})
export class OffreForm implements OnChanges, OnInit {
  @Input() admin = false;
  @Input() offre: Offre | null = null;
  @Input() enCours = false;
  @Input() erreur = '';
  @Output() valider = new EventEmitter<OffreRequest>();
  @Output() annuler = new EventEmitter<void>();
  private readonly utilisateurs = inject(UserService);
  private readonly references = inject(ReferencesService);
  readonly tests = signal<ChoixReference[]>([]);
  readonly recruteurs = signal<User[]>([]);
  readonly chargement = signal(false);
  readonly erreurChoix = signal('');
  readonly statuts: OffreStatut[] = ['BROUILLON', 'PUBLIEE', 'FERMEE'];
  competences = ''; // Saisie simple séparée par des virgules.
  readonly aujourdHui = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  formulaire: OffreRequest = {
    titre: '', description: '', competences: [], localisation: '', contrat: 'CDI',
    salaire: null, dateExpiration: '', statut: 'BROUILLON', nomEntreprise: '', entrepriseId: null, testRequisId: null
  };

  ngOnInit(): void {
    this.chargement.set(true);
    forkJoin({ references: this.references.charger(), users: this.admin && !this.offre ? this.utilisateurs.list() : of([]) }).subscribe({
      next: ({ references, users }) => {
        // Conserve les associations des anciennes offres sans inventer leur nom.
        if (this.offre?.testRequisId && !references.tests.some(t => t.id === this.offre!.testRequisId))
          references.tests.push({ id: this.offre.testRequisId, nom: 'Test associé à cette ancienne offre' });
        this.tests.set(references.tests);
        this.recruteurs.set(users.filter(u => u.role === 'RECRUTEUR'));
        this.chargement.set(false);
      },
      error: () => { this.erreurChoix.set('Impossible de charger les choix. Fermez puis réessayez.'); this.chargement.set(false); }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['offre'] || !this.offre) return;
    const { id, recruteurId, ...donnees } = this.offre; // Ne renvoie pas les identifiants protégés.
    this.formulaire = { ...donnees, competences: [...donnees.competences] };
    this.competences = donnees.competences.join(', ');
  }

  erreurDate(): string { // Même cohérence de statut et de date que le backend.
    const { dateExpiration, statut } = this.formulaire;
    if (statut === 'EXPIREE') return 'Choisissez un autre statut et une date actuelle ou future pour modifier cette offre.';
    if (!dateExpiration) return "La date d'expiration est obligatoire.";
    if (dateExpiration < this.aujourdHui) return 'Choisissez aujourd’hui ou une date future.';
    return '';
  }

  enregistrer(valide = true): void {
    if (!valide || this.erreurDate() || !this.formulaire.titre.trim() || this.enCours || this.chargement() || this.erreurChoix()) return;
    const competences = this.competences.split(',').map(valeur => valeur.trim()).filter(Boolean);
    if (!competences.length) return;
    this.valider.emit({ ...this.formulaire, titre: this.formulaire.titre.trim(), nomEntreprise: (this.formulaire.nomEntreprise || '').trim(), description: (this.formulaire.description || '').trim(), localisation: (this.formulaire.localisation || '').trim(), competences: [...new Set(competences)] });
  }
}
