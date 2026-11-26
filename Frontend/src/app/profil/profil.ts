import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, EventEmitter, inject, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../user/auth.service';
import { Profil, ProfilUpdate } from './profil.model';
import { ProfilService } from './profil.service';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profil.html',
  styleUrl: './profil.css'
})
export class ProfilPage {
  @Output() close = new EventEmitter<void>();

  private readonly profilService = inject(ProfilService);
  private readonly auth = inject(AuthService);

  readonly currentUser = this.auth.currentUser;
  readonly profil = signal<Profil | null>(null);
  readonly exists = signal(true);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly titre = signal('');
  readonly localisation = signal('');
  readonly competences = signal('');
  readonly experiences = signal('');
  readonly badges = signal('');

  readonly completude = computed(() => this.profil()?.completude ?? 0);
  readonly nomAffiche = computed(() => {
    const user = this.currentUser();
    if (!user) return 'Mon profil';
    return user.nom || user.email;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    if (!this.auth.isLoggedIn()) {
      this.loading.set(false);
      this.error.set('Connectez-vous pour consulter votre profil.');
      return;
    }

    this.profilService.getMe().subscribe({
      next: (profil) => {
        this.profil.set(profil);
        this.exists.set(true);
        this.apply(profil);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.exists.set(false);
          this.profil.set(null);
          this.error.set("Aucun profil n'existe encore pour votre compte. Remplissez le formulaire pour le créer.");
          return;
        }
        this.error.set(this.errorMessage(err));
      }
    });
  }

  save(): void {
    if (this.saving()) return;

    this.error.set('');
    this.success.set('');

    const validationError = this.validate();
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    const payload = this.payload();
    const userId = this.currentUser()?.id;
    if (!userId) {
      this.error.set('Impossible d’enregistrer le profil : utilisateur non authentifié.');
      return;
    }

    this.saving.set(true);
    const request$ = this.exists()
      ? this.profilService.updateMe(payload)
      : this.profilService.create({ ...payload, userId });

    request$.subscribe({
      next: (profil) => {
        this.profil.set(profil);
        this.exists.set(true);
        this.apply(profil);
        this.saving.set(false);
        this.success.set('Votre profil a été enregistré avec succès.');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(this.errorMessage(err));
      }
    });
  }

  exit(): void {
    this.close.emit();
  }

  competencesList(): string[] {
    return this.toList(this.competences());
  }

  badgesList(): string[] {
    return this.toList(this.badges());
  }

  private apply(profil: Profil): void {
    this.titre.set(profil.titre ?? '');
    this.localisation.set(profil.localisation ?? '');
    this.competences.set((profil.competences ?? []).join(', '));
    this.experiences.set((profil.experiences ?? []).join(', '));
    this.badges.set((profil.badges ?? []).join(', '));
  }

  private payload(): ProfilUpdate {
    return {
      titre: this.trimToNull(this.titre()),
      localisation: this.trimToNull(this.localisation()),
      competences: this.toList(this.competences()),
      experiences: this.toList(this.experiences()),
      badges: this.toList(this.badges())
    };
  }

  private validate(): string {
    if (this.titre().trim().length > 150) return 'Le titre ne doit pas dépasser 150 caractères.';
    if (this.localisation().trim().length > 150) return 'La localisation ne doit pas dépasser 150 caractères.';
    if (this.competencesList().some((value) => value.length > 100)) {
      return 'Chaque compétence ne doit pas dépasser 100 caractères.';
    }
    if (this.toList(this.experiences()).some((value) => value.length > 250)) {
      return 'Chaque expérience ne doit pas dépasser 250 caractères.';
    }
    if (this.badgesList().some((value) => value.length > 100)) {
      return 'Chaque badge ne doit pas dépasser 100 caractères.';
    }
    return '';
  }

  private toList(value: string): string[] {
    return [...new Set(value.split(',').map((item) => item.trim()).filter(Boolean))];
  }

  private trimToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private errorMessage(err: HttpErrorResponse): string {
    const body = err.error as { message?: string; error?: string } | null;
    if (err.status === 401) return 'Votre session a expiré. Connectez-vous à nouveau.';
    if (err.status === 403) return "Vous n'avez pas accès à ce profil.";
    return body?.message ?? body?.error ?? err.message ?? "Une erreur est survenue lors de l'appel à l'API.";
  }
}
