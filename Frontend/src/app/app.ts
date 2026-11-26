import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from './user/auth.service';
import { Role } from './user/user.model';
import { BackOffice } from './backoffice/backoffice';
import { Avis, Entreprise, SubmitAvisRequest } from './entreprises/entreprise.model';
import { EntrepriseService } from './entreprises/entreprise.service';

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
  imports: [CommonModule, FormsModule, BackOffice],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
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
  readonly favorites = signal<number[]>([]);
  readonly selectedJob = signal<Job | null>(null);
  readonly authMode = signal<'connexion' | 'inscription' | null>(null);
  readonly infoMessage = signal('');
  private readonly auth = inject(AuthService);
  private readonly entrepriseService = inject(EntrepriseService);
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
  readonly entreprises = signal<Entreprise[]>([]);
  readonly entreprisesLoading = signal(false);
  readonly entreprisesError = signal('');
  readonly entrepriseSecteur = signal('');
  readonly entrepriseVille = signal('');
  readonly secteurs = computed(() => [...new Set(this.entreprises().map((entreprise) => entreprise.secteur))].sort());
  readonly villes = computed(() => [...new Set(this.entreprises().map((entreprise) => entreprise.ville))].sort());
  readonly filteredEntreprises = computed(() => {
    const secteur = this.entrepriseSecteur();
    const ville = this.entrepriseVille();
    return this.entreprises().filter((entreprise) => (!secteur || entreprise.secteur === secteur) && (!ville || entreprise.ville === ville));
  });
  readonly avisModal = signal<{ mode: 'list' | 'form'; entreprise: Entreprise } | null>(null);
  readonly selectedEntrepriseAvis = signal<Avis[]>([]);
  readonly approvedAvis = computed(() => this.selectedEntrepriseAvis().filter((avis) => avis.statut === 'APPROUVE'));
  readonly avisLoading = signal(false);
  readonly avisSaving = signal(false);
  readonly avisNote = signal(5);
  readonly avisCommentaire = signal('');
  readonly avisError = signal('');
  readonly avisSuccess = signal('');
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
  readonly count = computed(() => this.jobs.filter(job => !this.domain() || job.domain === this.domain()).length);
  ngOnInit(): void { this.loadEntreprises(); }
  toggleNav(id: string): void { this.openNav.update((current) => current === id ? null : id); }
  closeNav(): void { this.openNav.set(null); }
  toggleMobileNav(): void { this.mobileNavOpen.update((open) => !open);this.openNav.set(null); }
  @HostListener('document:click')
  onDocumentClick(): void { this.closeNav();this.notificationsOpen.set(false); }
  @HostListener('document:keydown.escape')
  onEscape(): void { this.closeNav();this.mobileNavOpen.set(false);this.notificationsOpen.set(false);this.closeModal(); }
  toggleNotifications(): void { this.notificationsOpen.update((open) => !open);this.closeNav();this.mobileNavOpen.set(false); }
  markAllRead(): void { this.unreadCount.set(0); }
  onNotification(notif: { title: string; detail: string }): void {
    this.notificationsOpen.set(false);
    this.showMessage(`${notif.title} — ${notif.detail}`);
  }
  onNavAction(serviceId: string, label: string): void {
    this.closeNav();this.mobileNavOpen.set(false);this.notificationsOpen.set(false);
    const service = this.navServices.find((item) => item.id === serviceId);
    if (serviceId === 'entreprises') {
      document.getElementById('entreprises')?.scrollIntoView({ behavior: 'smooth' });
      this.showMessage(`${service?.full ?? serviceId} · ${label} — annuaire public connecté à l'API.`);
      return;
    }
    this.showMessage(`${service?.full ?? serviceId} · ${label} — à brancher sur l'API Spring Boot.`);
    if (serviceId === 'offres') { document.getElementById('offres')?.scrollIntoView({ behavior: 'smooth' }); }
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
  toggleFavorite(jobId: number): void {
    this.favorites.update((ids) => ids.includes(jobId) ? ids.filter(id => id !== jobId) : [...ids, jobId]);
  }
  openJob(job: Job): void { this.selectedJob.set(job);this.authMode.set(null);this.infoMessage.set(''); }
  closeModal(): void {
    this.selectedJob.set(null);
    this.authMode.set(null);
    this.infoMessage.set('');
    this.authError.set('');
    this.authLoading.set(false);
    this.avisModal.set(null);
    this.selectedEntrepriseAvis.set([]);
    this.avisLoading.set(false);
    this.avisSaving.set(false);
    this.avisNote.set(5);
    this.avisCommentaire.set('');
    this.avisError.set('');
    this.avisSuccess.set('');
  }

  openAuth(mode: 'connexion' | 'inscription'): void {
    this.closeModal();
    this.authMode.set(mode);
  }

  loadEntreprises(): void {
    this.entreprisesLoading.set(true);
    this.entreprisesError.set('');
    this.entrepriseService.list().subscribe({
      next: (entreprises) => {
        this.entreprises.set(entreprises);
        this.entreprisesLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.entreprisesLoading.set(false);
        this.entreprisesError.set(this.readError(error));
      }
    });
  }

  resetEntrepriseFilters(): void { this.entrepriseSecteur.set('');this.entrepriseVille.set(''); }

  formatNote(note: number): string { return note.toFixed(2).replace('.', ','); }

  showAvis(entreprise: Entreprise): void {
    this.selectedJob.set(null);
    this.authMode.set(null);
    this.avisModal.set({ mode: 'list', entreprise });
    this.avisError.set('');
    this.avisSuccess.set('');
    this.loadAvis(entreprise);
  }

  openAvisForm(entreprise: Entreprise): void {
    if (!this.isLoggedIn()) {
      this.openAuth('connexion');
      this.infoMessage.set('Connectez-vous pour publier un avis.');
      return;
    }
    if (this.isAdmin()) {
      this.showMessage('Un administrateur ne peut pas publier d’avis.');
      return;
    }
    this.selectedJob.set(null);
    this.authMode.set(null);
    this.avisModal.set({ mode: 'form', entreprise });
    this.avisNote.set(5);
    this.avisCommentaire.set('');
    this.avisError.set('');
    this.avisSuccess.set('');
  }

  setAvisNote(value: string): void { this.avisNote.set(Number(value)); }

  submitAvis(): void {
    const modal = this.avisModal();
    const note = this.avisNote();
    const commentaire = this.avisCommentaire().trim();
    if (!modal || modal.mode !== 'form' || this.avisSaving()) { return; }
    if (!Number.isInteger(note) || note < 1 || note > 5) {
      this.avisError.set('La note doit être comprise entre 1 et 5.');
      return;
    }
    if (!commentaire) {
      this.avisError.set('Le commentaire est obligatoire.');
      return;
    }
    this.avisSaving.set(true);
    this.avisError.set('');
    this.avisSuccess.set('');
    const request: SubmitAvisRequest = { note, commentaire };
    this.entrepriseService.submitAvis(modal.entreprise.id, request).subscribe({
      next: () => {
        this.avisSaving.set(false);
        this.avisCommentaire.set('');
        this.avisSuccess.set('Votre avis a été envoyé en modération.');
        this.loadAvis(modal.entreprise);
        this.loadEntreprises();
      },
      error: (error: HttpErrorResponse) => {
        this.avisSaving.set(false);
        this.avisError.set(error.status === 409
          ? 'Vous avez déjà publié un avis pour cette entreprise.'
          : this.readError(error));
      }
    });
  }

  private loadAvis(entreprise: Entreprise): void {
    this.avisLoading.set(true);
    this.avisError.set('');
    this.selectedEntrepriseAvis.set([]);
    this.entrepriseService.listAvis(entreprise.id).subscribe({
      next: (avis) => {
        this.selectedEntrepriseAvis.set(avis);
        this.avisLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.avisLoading.set(false);
        this.avisError.set(this.readError(error));
      }
    });
  }

  private readError(error: HttpErrorResponse): string {
    return error.error?.message ?? error.error?.detail ?? error.message ?? 'Service indisponible pour le moment.';
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
