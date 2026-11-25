import { CommonModule } from '@angular/common';
import { Component, computed, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from './user/auth.service';
import { Role } from './user/user.model';
import { BackOffice } from './backoffice/backoffice';
import { AlertesService, alertesErrorMessage } from './alertes/alertes.service';
import { AlertesView, FrontofficeAlertes } from './alertes/frontoffice-alertes';

type Domain = 'Développement' | 'Data' | 'Ressources humaines' | 'Design' | 'Marketing';
type Contract = 'CDI' | 'CDD' | 'Stage' | 'Freelance';
interface Job {
  id: number;
  title: string;
  company: string;
  location: string;
  contract: Contract;
  domain: Domain;
  salary: string;
  tags: string[];
  icon: string;
  blurb: string;
}
interface NavService {
  id: string;
  label: string;
  full: string;
  items: string[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, BackOffice, FrontofficeAlertes],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  // Données fictives de démonstration : remplacer par votre microservice Offres & Candidatures.
  readonly jobs: Job[] = [
    { id: 1, title: 'Développeur Full Stack', company: 'TechNova', location: 'Tunis', contract: 'CDI', domain: 'Développement', salary: '2 800 – 3 800 DT', tags: ['Angular', 'Spring Boot', 'Java'], icon: 'code', blurb: 'Participez au développement de solutions web modernes en équipe.' },
    { id: 2, title: 'Data Analyst', company: 'DataPlus', location: 'Sfax', contract: 'CDD', domain: 'Data', salary: '2 000 – 3 000 DT', tags: ['SQL', 'Power BI', 'Python'], icon: 'chart', blurb: 'Transformez des données en décisions et créez des tableaux de bord.' },
    { id: 3, title: 'Responsable RH', company: 'Talent RH', location: 'Tunis', contract: 'CDI', domain: 'Ressources humaines', salary: 'Selon profil', tags: ['Recrutement', 'Gestion RH', 'Communication'], icon: 'team', blurb: 'Accompagnez les recrutements et valorisez les talents de demain.' },
    { id: 4, title: 'Développeur Java Backend', company: 'CodeSphere', location: 'Sousse', contract: 'Stage', domain: 'Développement', salary: 'Stage rémunéré', tags: ['Java', 'Spring Boot', 'API REST'], icon: 'code', blurb: 'Concevez des API et découvrez une architecture microservices.' },
    { id: 5, title: 'UX/UI Designer', company: 'PixelLab', location: 'Tunis', contract: 'Freelance', domain: 'Design', salary: 'Selon mission', tags: ['Figma', 'UX Research', 'Design'], icon: 'design', blurb: 'Imaginez des interfaces accessibles et intuitives.' },
    { id: 6, title: 'Chargé de marketing digital', company: 'Grow&Co', location: 'Ariana', contract: 'CDD', domain: 'Marketing', salary: 'Selon profil', tags: ['SEO', 'Réseaux sociaux', 'Analytics'], icon: 'chart', blurb: 'Développez la visibilité digitale de marques innovantes.' }
  ];
  readonly navServices: NavService[] = [
    {
      id: 'offres',
      label: 'Offres',
      full: 'Offres & Candidatures',
      items: [
        'Rechercher une offre',
        'Offres recommandées',
        'Déposer une candidature',
        'Suivre mes candidatures',
        'Planifier un entretien'
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
  readonly domain = signal('');
  readonly location = signal('');
  readonly showAll = signal(false);
  readonly selectedJob = signal<Job | null>(null);
  readonly authMode = signal<'connexion' | 'inscription' | null>(null);
  readonly infoMessage = signal('');
  private readonly auth = inject(AuthService);
  private readonly alertesApi = inject(AlertesService);
  /** Page « Mes alertes » / « Mes favoris » affichée à la place de l'accueil (null = accueil). */
  readonly alertesView = signal<AlertesView | null>(null);
  /** Ids des offres en favori, issus de GET /skillmatch/favorites/me. */
  readonly favorites = this.alertesApi.favoriOfferIds;
  readonly isCandidat = computed(() => this.isLoggedIn() && this.currentUser()?.role === 'CANDIDAT');
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
  readonly filteredJobs = computed(() => {
    const q = this.searchTerm().trim().toLowerCase();
    const d = this.domain();
    const loc = this.location();
    const matching = this.jobs.filter((job) => {
      const keywordMatch = !q || [job.title,job.company,...job.tags].some((part) => part.toLowerCase().includes(q));
      return keywordMatch && (!d || job.domain === d) && (!loc || job.location === loc);
    });
    return this.showAll() || q || d || loc ? matching : matching.slice(0, 3);
  });
  constructor() {
    if (this.isCandidat()) { this.loadFavorites(); }
  }
  readonly count = computed(() => this.jobs.filter(job => !this.domain() || job.domain === this.domain()).length);
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
    if (serviceId === 'alertes') { this.onAlertesAction(label); return; }
    const service = this.navServices.find((item) => item.id === serviceId);
    this.showMessage(`${service?.full ?? serviceId} · ${label} — à brancher sur l'API Spring Boot.`);
    if (serviceId === 'offres') { this.showOffers(); }
  }
  private onAlertesAction(label: string): void {
    if (label === 'Sauvegarder une offre') {
      this.openHome();
      this.showMessage('Cliquez sur ♡ d\'une offre pour la sauvegarder dans vos favoris.');
      window.setTimeout(() => document.getElementById('offres')?.scrollIntoView({ behavior: 'smooth' }));
      return;
    }
    this.openAlertes(label === 'Mes favoris' ? 'favoris' : 'alertes');
  }
  openAlertes(vue: AlertesView): void {
    this.alertesView.set(vue);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  openHome(): void { this.alertesView.set(null); }
  showOffers(): void {
    this.openHome();
    window.setTimeout(() => document.getElementById('offres')?.scrollIntoView({ behavior: 'smooth' }));
  }
  private loadFavorites(): void {
    this.alertesApi.chargerFavoris().subscribe({ error: () => this.alertesApi.viderFavoris() });
  }
  private showMessage(text: string): void {
    this.navMessage.set(text);
    window.clearTimeout(this.navMessageTimer);
    this.navMessageTimer = window.setTimeout(() => this.navMessage.set(''), 5000);
  }
  onSearch(): void {
    this.showAll.set(true);
    document.getElementById('offres')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  resetSearch(): void { this.searchTerm.set('');this.domain.set('');this.location.set('');this.showAll.set(false); }
  showAllJobs(): void { this.resetSearch();this.showAll.set(true);document.getElementById('offres')?.scrollIntoView({behavior:'smooth'}); }
  isFavorite(jobId: number): boolean { return this.favorites().includes(jobId); }
  /** Bouton ♡ : POST /skillmatch/favorites ou DELETE /skillmatch/favorites/{id} (rôle CANDIDAT). */
  toggleFavorite(jobId: number): void {
    if (!this.isLoggedIn()) {
      this.openAuth('connexion');
      this.infoMessage.set('Connectez-vous avec un compte candidat pour sauvegarder une offre.');
      return;
    }
    if (!this.isCandidat()) {
      this.showMessage('Les favoris sont réservés aux comptes candidats.');
      return;
    }
    this.alertesApi.basculerFavori(jobId).subscribe({
      next: (added) => this.showMessage(added ? 'Offre ajoutée à vos favoris.' : 'Offre retirée de vos favoris.'),
      error: (error) => this.showMessage(alertesErrorMessage(error, 'Impossible de mettre à jour vos favoris.'))
    });
  }
  openJob(job: Job): void { this.selectedJob.set(job);this.authMode.set(null);this.infoMessage.set(''); }
  closeModal(): void {
    this.selectedJob.set(null);
    this.authMode.set(null);
    this.infoMessage.set('');
    this.authError.set('');
    this.authLoading.set(false);
  }

  openAuth(mode: 'connexion' | 'inscription'): void {
    this.authMode.set(mode);
    this.selectedJob.set(null);
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
        if (this.isCandidat()) { this.loadFavorites(); }
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
    this.alertesApi.viderFavoris();
    this.alertesView.set(null);
    this.showMessage('Vous êtes déconnecté.');
  }

  openBackOffice(): void { this.backOfficeOpen.set(true); }

  closeBackOffice(): void { this.backOfficeOpen.set(false); }

  apply(): void {
    if (!this.isLoggedIn()) {
      this.selectedJob.set(null);
      this.openAuth('connexion');
      this.infoMessage.set('Connectez-vous pour déposer votre candidature.');
      return;
    }
    const title = this.selectedJob()?.title ?? '';
    this.closeModal();
    this.showMessage(`Candidature prête pour « ${title} » — à brancher sur POST /skillmatch/candidatures/add.`);
  }
}
