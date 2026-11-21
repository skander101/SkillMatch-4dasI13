import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from './user/auth.service';
import { Role } from './user/user.model';
import { BackOffice } from './backoffice/backoffice';
import { Offres } from './offres/offres'; // Liste connectée à l’API existante.

interface NavService {
  id: string;
  label: string;
  full: string;
  items: string[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, BackOffice, Offres],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly navServices: NavService[] = [
    {
      id: 'offres',
      label: 'Offres',
      full: 'Offres & Candidatures',
      items: [
        'Voir les offres'
      ]
    },
    {
      id: 'profils',
      label: 'Profils',
      full: 'Profils & CV',
      items: [
        'Compléter mon profil',
        'Compétences & badges',
        'Créer mon CV',
        'Complétude du profil'
      ]
    },
    {
      id: 'entreprises',
      label: 'Entreprises',
      full: 'Entreprises & Avis',
      items: [
        'Annuaire des entreprises',
        'Publier un avis',
        'Consulter les évaluations',
        'Modérer les avis'
      ]
    },
    {
      id: 'tests',
      label: 'Tests',
      full: 'Tests & Compétences',
      items: [
        'Passer un test technique',
        'Correction automatique',
        'Mes résultats',
        'Tentatives restantes'
      ]
    },
    {
      id: 'alertes',
      label: 'Alertes',
      full: 'Alertes & Favoris',
      items: [
        'Créer une alerte emploi',
        'Gérer mes alertes',
        'Sauvegarder une offre',
        'Mes favoris'
      ]
    },
    {
      id: 'messagerie',
      label: 'Messagerie',
      full: 'Messagerie',
      items: [
        'Conversations',
        'Envoyer un message',
        'Historique des messages',
        'Messagerie recruteur'
      ]
    }
  ];
  readonly openNav = signal<string | null>(null);
  readonly mobileNavOpen = signal(false);
  readonly navMessage = signal('');
  readonly notificationsOpen = signal(false);
  readonly unreadCount = signal(4);
  readonly notifications = [
    { tag: 'CANDIDATURE', title: 'Candidature acceptée', detail: 'TechNova · Développeur Full Stack', time: 'il y a 2 h' },
    { tag: 'TEST', title: 'Compétence validée', detail: 'Test Angular réussi · badge obtenu', time: 'hier' },
    { tag: 'ALERTE', title: 'Nouvelle offre correspondante', detail: 'Data Analyst · Sfax', time: 'hier' },
    { tag: 'MESSAGE', title: 'Nouveau message', detail: 'Talent RH vous a écrit', time: 'il y a 3 j' }
  ];
  private navMessageTimer?: number;
  readonly searchTerm = signal('');
  readonly rechercheOffres = signal(''); // Recherche envoyée lorsque l'utilisateur valide.
  readonly offreSort = signal('titre');
  readonly authMode = signal<'connexion' | 'inscription' | null>(null);
  readonly infoMessage = signal('');
  private readonly auth = inject(AuthService);
  readonly currentUser = this.auth.currentUser;
  readonly isLoggedIn = this.auth.isLoggedIn;
  readonly isAdmin = this.auth.isAdmin;
  readonly backOfficeOpen = signal(this.auth.isAdmin());
  readonly authEmail = signal('');
  readonly authPassword = signal('');
  readonly authName = signal('');
  readonly authRole = signal<Role>('CANDIDAT');
  readonly authError = signal('');
  readonly authLoading = signal(false);
  toggleNav(id: string): void { this.openNav.update((current) => current === id ? null : id); }
  closeNav(): void { this.openNav.set(null); }
  toggleMobileNav(): void { this.mobileNavOpen.update((open) => !open);this.openNav.set(null); }
  @HostListener('document:click')
  onDocumentClick(): void { this.closeNav();this.notificationsOpen.set(false); }
  @HostListener('document:keydown.escape')
  onEscape(): void { this.closeNav();this.mobileNavOpen.set(false);this.notificationsOpen.set(false); }
  toggleNotifications(): void { this.notificationsOpen.update((open) => !open);this.closeNav();this.mobileNavOpen.set(false); }
  markAllRead(): void { this.unreadCount.set(0); }
  onNotification(notif: { title: string; detail: string }): void {
    this.notificationsOpen.set(false);
    this.showMessage(`${notif.title} — ${notif.detail}`);
  }
  onNavAction(serviceId: string, label: string): void {
    this.closeNav();this.mobileNavOpen.set(false);this.notificationsOpen.set(false);
    if (serviceId === 'offres') { this.onSearch(); return; } // Ouvre la vraie liste selon le rôle.
    const service = this.navServices.find((item) => item.id === serviceId);
    this.showMessage(`${service?.full ?? serviceId} · ${label} — à brancher sur l'API Spring Boot.`);
  }
  private showMessage(text: string): void {
    this.navMessage.set(text);
    window.clearTimeout(this.navMessageTimer);
    this.navMessageTimer = window.setTimeout(() => this.navMessage.set(''), 5000);
  }
  onSearch(): void {
    this.rechercheOffres.set(this.searchTerm().trim());
    document.getElementById('offres')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (!this.isLoggedIn()) this.openAuth('connexion'); // Réutilise le login existant.
  }

  closeModal(): void {
    this.authMode.set(null);
    this.infoMessage.set('');
    this.authError.set('');
    this.authLoading.set(false);
  }

  openAuth(mode: 'connexion' | 'inscription'): void {
    this.authMode.set(mode);
    this.infoMessage.set('');
    this.authError.set('');
    this.authLoading.set(false);
  }

  setRole(value: string): void {
    this.authRole.set(value as Role);
  }

  submitAuth(mode: 'connexion' | 'inscription'): void {
    this.authError.set('');
    this.authLoading.set(true);
    const request$ = mode === 'connexion'
      ? this.auth.login({ email: this.authEmail(), password: this.authPassword() })
      : this.auth.register({
          nom: this.authName(),
          email: this.authEmail(),
          password: this.authPassword(),
          role: this.authRole()
        });

    request$.subscribe({
      next: () => {
        this.authLoading.set(false);
        this.closeModal();
        if (this.isAdmin()) { this.backOfficeOpen.set(true); }
        this.showMessage(`Bienvenue ${this.currentUser()?.nom ?? ''} — rôle ${this.currentUser()?.role ?? ''}`);
      },
      error: (error) => {
        this.authLoading.set(false);
        this.authError.set(error?.error?.message ?? error?.error?.detail ?? error?.message ?? 'Service utilisateur indisponible pour le moment.');
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.backOfficeOpen.set(false);
    this.showMessage('Vous êtes déconnecté.');
  }

  openBackOffice(): void { this.backOfficeOpen.set(true); }

  closeBackOffice(): void { this.backOfficeOpen.set(false); }

}
