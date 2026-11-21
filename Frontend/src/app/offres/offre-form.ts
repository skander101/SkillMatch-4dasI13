import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Offre, OffreRequest, OffreStatut } from './offre.model';

@Component({
  selector: 'app-offre-form',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './offre-form.html',
  styleUrls: ['../app.css', './offres.css'] // Réutilise le thème et les modales du template.
})
export class OffreForm implements OnChanges {
  @Input() offre: Offre | null = null;
  @Input() enCours = false;
  @Input() erreur = '';
  @Output() valider = new EventEmitter<OffreRequest>();
  @Output() annuler = new EventEmitter<void>();
  readonly statuts: OffreStatut[] = ['BROUILLON', 'PUBLIEE', 'FERMEE', 'EXPIREE'];
  competences = ''; // Saisie simple séparée par des virgules.
  formulaire: OffreRequest = {
    titre: '', description: '', competences: [], localisation: '', contrat: 'CDI',
    salaire: null, dateExpiration: '', statut: 'BROUILLON', entrepriseId: null, testRequisId: null
  };

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
