import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Offre, OffreRequest, OffreStatut } from './offre.model';
import { UserService } from '../user/user.service';
import { User } from '../user/user.model';

@Component({
  selector: 'app-offre-form',
  standalone: true,
  imports: [FormsModule],
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
  readonly recruteurs = signal<User[]>([]);
  readonly chargement = signal(false);
  readonly erreurChoix = signal('');
  readonly statuts: OffreStatut[] = ['BROUILLON', 'PUBLIEE', 'FERMEE', 'EXPIREE'];
  competences = ''; // Saisie simple séparée par des virgules.
  formulaire: OffreRequest = {
    titre: '', description: '', competences: [], localisation: '', contrat: 'CDI',
    salaire: null, dateExpiration: '', statut: 'BROUILLON', entrepriseId: null, testRequisId: null
  };

  ngOnInit(): void { // ADMIN choisit un recruteur réel pour la création.
    if (!this.admin || this.offre) return;
    this.chargement.set(true);
    this.utilisateurs.list().subscribe({
      next: users => { this.recruteurs.set(users.filter(u => u.role === 'RECRUTEUR')); this.chargement.set(false); },
      error: () => { this.erreurChoix.set('Impossible de charger les recruteurs. Fermez puis réessayez.'); this.chargement.set(false); }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['offre'] || !this.offre) return;
    const { id, recruteurId, ...donnees } = this.offre; // Ne renvoie pas les identifiants protégés.
    this.formulaire = { ...donnees, competences: [...donnees.competences] };
    this.competences = donnees.competences.join(', ');
  }

  enregistrer(): void {
    const competences = this.competences.split(',').map(valeur => valeur.trim()).filter(Boolean);
    this.valider.emit({ ...this.formulaire, titre: this.formulaire.titre.trim(), competences: [...new Set(competences)] });
  }
}
