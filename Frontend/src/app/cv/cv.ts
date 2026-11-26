import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { AuthService } from '../user/auth.service';
import { Cv } from './cv.model';
import { CvService } from './cv.service';

@Component({
  selector: 'app-cv',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cv.html',
  styleUrl: './cv.css'
})
export class CvPage {
  @Output() close = new EventEmitter<void>();
  @Output() openProfile = new EventEmitter<void>();

  private readonly cvService = inject(CvService);
  private readonly auth = inject(AuthService);

  readonly cvs = signal<Cv[]>([]);
  readonly selectedFile = signal<File | null>(null);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly maxSize = 5 * 1024 * 1024;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    if (!this.auth.isLoggedIn()) {
      this.loading.set(false);
      this.error.set('Connectez-vous pour gérer votre CV.');
      return;
    }

    this.cvService.list().subscribe({
      next: (cvs) => {
        this.cvs.set(cvs);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.error.set('Créez d’abord votre profil pour pouvoir déposer un CV.');
          return;
        }
        this.error.set(this.errorMessage(err));
      }
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.success.set('');
    this.error.set('');

    if (!file) {
      this.selectedFile.set(null);
      return;
    }
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      input.value = '';
      this.selectedFile.set(null);
      this.error.set('Le CV doit être un fichier PDF.');
      return;
    }
    if (file.size > this.maxSize) {
      input.value = '';
      this.selectedFile.set(null);
      this.error.set('Le fichier ne doit pas dépasser 5 Mo.');
      return;
    }
    this.selectedFile.set(file);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) return;

    this.error.set('');
    this.success.set('');
    this.uploading.set(true);

    this.cvService.upload(file).subscribe({
      next: (cv) => {
        this.cvs.update((list) => [cv, ...list]);
        this.selectedFile.set(null);
        this.uploading.set(false);
        this.success.set('CV déposé avec succès.');
      },
      error: (err: HttpErrorResponse) => {
        this.uploading.set(false);
        this.error.set(this.errorMessage(err));
      }
    });
  }

  remove(cv: Cv): void {
    if (this.deletingId() !== null || !window.confirm('Supprimer ce CV ?')) return;

    this.error.set('');
    this.success.set('');
    this.deletingId.set(cv.id);

    this.cvService.delete(cv.id).subscribe({
      next: () => {
        this.cvs.update((list) => list.filter((item) => item.id !== cv.id));
        this.deletingId.set(null);
        this.success.set('CV supprimé.');
      },
      error: (err: HttpErrorResponse) => {
        this.deletingId.set(null);
        this.error.set(this.errorMessage(err));
      }
    });
  }

  exit(): void {
    this.close.emit();
  }

  goToProfile(): void {
    this.openProfile.emit();
  }

  fileName(uri: string): string {
    return uri.split(/[\\/]/).pop() ?? uri;
  }

  formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('fr-FR');
  }

  private errorMessage(err: HttpErrorResponse): string {
    const body = err.error as { message?: string; error?: string } | null;
    if (err.status === 401) return 'Votre session a expiré. Connectez-vous à nouveau.';
    if (err.status === 403) return 'Vous n’avez pas accès à ce CV.';
    if (err.status === 413) return 'Le fichier CV dépasse la taille maximale autorisée.';
    return body?.message ?? body?.error ?? err.message ?? "Une erreur est survenue lors de l'appel à l'API.";
  }
}
