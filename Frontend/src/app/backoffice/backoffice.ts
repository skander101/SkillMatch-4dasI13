import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, EventEmitter, HostListener, inject, OnInit, Output, signal } from '@angular/core';
import { NAV_SERVICES, NOTIFICATIONS, AppNotification } from './nav.data';
import { AuthService } from '../user/auth.service';
import { CreateUserRequest, Role, UpdateUserRequest, User } from '../user/user.model';
import { UserService } from '../user/user.service';
import { Offres } from '../offres/offres';
import { Candidatures } from '../candidatures/candidatures';

interface SideItem {
  id: string;
  label: string;
  path: string;
}
interface SideEntity {
  id: string;
  label: string;
}
interface SideGroup extends SideItem {
  children: SideEntity[];
}
interface AdminRow {
  cells: string[];
  status: string;
  tone: string;
}
interface AdminTable {
  title: string;
  subtitle: string;
  columns: string[];
  rows: AdminRow[];
}

@Component({
  selector: 'app-back-office',
  standalone: true,
  imports: [CommonModule, Offres, Candidatures],
  templateUrl: './backoffice.html',
  styleUrl: './backoffice.css'
})
export class BackOffice implements OnInit {
  @Output() deconnexion = new EventEmitter<void>();

  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);

  readonly navServices = NAV_SERVICES;
  readonly notifications = NOTIFICATIONS;
  readonly currentUser = this.auth.currentUser;
  readonly gestionItems: SideItem[] = [
    { id: 'dashboard', label: 'Tableau de bord', path: 'M3 3h8v8H3zM13 3h8v5h-8zM13 10h8v11h-8zM3 13h8v8H3z' },
    { id: 'offres', label: 'Offres', path: 'M3 8h18v12H3zM8 8V5h8v3M3 13h18' },
    { id: 'candidatures', label: 'Candidatures', path: 'M6 3h9l4 4v14H6zM15 3v5h4M9 12h7M9 16h7' },
    { id: 'profils', label: 'Profils & CV', path: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21c0-4.5 3.6-7 8-7s8 2.5 8 7' },
    { id: 'entreprises', label: 'Entreprises & Avis', path: 'M4 21V5h10v16M14 10h6v11M7 9h2M7 13h2M7 17h2M17 14h1M17 18h1' },
    { id: 'tests', label: 'Tests & Compétences', path: 'M4 6l2 2 4-4M4 14l2 2 4-4M13 7h7M13 15h7' },
    { id: 'alertes', label: 'Alertes & Favoris', path: 'M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8ZM10 20a2 2 0 0 0 4 0' },
    { id: 'messagerie', label: 'Messagerie', path: 'M4 5h16v11H9l-5 4V5ZM8 10h8' }
  ];
  readonly adminItems: SideItem[] = [
    { id: 'notifications', label: 'Notifications', path: 'M5 21V4h12l-2 4 2 4H5' },
    { id: 'utilisateurs', label: 'Utilisateurs & rôles', path: 'M9 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM2 20c0-4 3-6 7-6s7 2 7 6M17 5a3 3 0 0 1 0 6M17 14c3 0 5 2 5 6' },
    { id: 'parametres', label: 'Paramètres', path: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19 12l2-1-2-3-2 .5-1-2 1-2-3-1-1 2-2 .5-.5-2-3 1 1 2-.5 2-2 1 2 3 2-.5 1 2-1 2 3 1 1-2 2-.5.5 2 3-1-1-2 2-1z' }
  ];
  readonly dashboardItem: SideItem = this.gestionItems[0];
  readonly sideGroups: SideGroup[] = [
    { id: 'offres', label: 'Offres & Candidatures', path: 'M3 8h18v12H3zM8 8V5h8v3M3 13h18', children: [
      { id: 'offres', label: 'Toutes les offres' },
      { id: 'candidatures', label: 'Toutes les candidatures' }
    ] },
    { id: 'profils', label: 'Profils & CV', path: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21c0-4.5 3.6-7 8-7s8 2.5 8 7', children: [
      { id: 'profils', label: 'Profils & CV' }
    ] },
    { id: 'entreprises', label: 'Entreprises & Avis', path: 'M4 21V5h10v16M14 10h6v11M7 9h2M7 13h2M7 17h2M17 14h1M17 18h1', children: [
      { id: 'entreprises', label: 'Entreprises' },
      { id: 'entreprises', label: 'Avis à modérer' }
    ] },
    { id: 'tests', label: 'Tests & Compétences', path: 'M4 6l2 2 4-4M4 14l2 2 4-4M13 7h7M13 15h7', children: [
      { id: 'tests', label: 'Tests' },
      { id: 'tests', label: 'Résultats' }
    ] },
    { id: 'alertes', label: 'Alertes & Favoris', path: 'M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8ZM10 20a2 2 0 0 0 4 0', children: [
      { id: 'alertes', label: 'Alertes' },
      { id: 'alertes', label: 'Favoris' }
    ] },
    { id: 'messagerie', label: 'Messagerie', path: 'M4 5h16v11H9l-5 4V5ZM8 10h8', children: [
      { id: 'messagerie', label: 'Conversations' },
      { id: 'messagerie', label: 'Messages signalés' }
    ] }
  ];
  readonly adminGroups: SideGroup[] = [
    { id: 'administration', label: 'Administration', path: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19 12l2-1-2-3-2 .5-1-2 1-2-3-1-1 2-2 .5-.5-2-3 1 1 2-.5 2-2 1 2 3 2-.5 1 2-1 2 3 1 1-2 2-.5.5 2 3-1-1-2 2-1z', children: [
      { id: 'notifications', label: 'Notifications envoyées' },
      { id: 'utilisateurs', label: 'Utilisateurs & rôles' },
      { id: 'parametres', label: 'Paramètres' }
    ] }
  ];
  readonly kpis = [
    { label: 'Offres actives', value: '128', trend: '+12 %', tone: 'up', note: 'ce mois-ci' },
    { label: 'Candidatures reçues', value: '342', trend: '+8 %', tone: 'up', note: 'cette semaine' },
    { label: 'Tests réussis', value: '87 %', trend: '+4 pts', tone: 'up', note: 'sur 30 jours' },
    { label: 'Entretiens planifiés', value: '24', trend: '-3 %', tone: 'down', note: 'vs. semaine dernière' }
  ];
  readonly bars = [
    { label: 'Lun', value: 42 }, { label: 'Mar', value: 68 }, { label: 'Mer', value: 55 },
    { label: 'Jeu', value: 82 }, { label: 'Ven', value: 64 }, { label: 'Sam', value: 30 }, { label: 'Dim', value: 22 }
  ];
  readonly activities = [
    { tag: 'StatusChanged', text: 'Candidature de Lina Ben Ali acceptée chez TechNova', time: 'il y a 12 min' },
    { tag: 'OfferPublished', text: 'Nouvelle offre « Data Analyst » publiée par DataPlus', time: 'il y a 45 min' },
    { tag: 'TestPassed', text: 'Badge Angular décerné à Sami Trabelsi', time: 'il y a 2 h' },
    { tag: 'MessageSent', text: 'Nouvelle conversation ouverte avec Talent RH', time: 'il y a 3 h' }
  ];
  readonly tables: Record<string, AdminTable> = {
    offres: {
      title: 'Offres publiées',
      subtitle: 'Suivi des offres par les entreprises partenaires',
      columns: ['Offre', 'Entreprise', 'Localisation', 'Contrat', 'Publiée le', 'Statut', 'Actions'],
      rows: [
        { cells: ['Développeur Full Stack', 'TechNova', 'Tunis', 'CDI', '12/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['Data Analyst', 'DataPlus', 'Sfax', 'CDD', '10/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['UX/UI Designer', 'PixelLab', 'Tunis', 'Freelance', '08/11/2026'], status: 'En pause', tone: 'amber' },
        { cells: ['Développeur Java Backend', 'CodeSphere', 'Sousse', 'Stage', '02/11/2026'], status: 'Expirée', tone: 'gray' },
        { cells: ['Chargé marketing digital', 'Grow&Co', 'Ariana', 'CDD', '28/10/2026'], status: 'Active', tone: 'green' }
      ]
    },
    candidatures: {
      title: 'Candidatures',
      subtitle: 'Pipeline de recrutement et changements de statut',
      columns: ['Candidat', 'Offre', 'Date', 'Prochaine étape', 'Statut', 'Actions'],
      rows: [
        { cells: ['Lina Ben Ali', 'Développeur Full Stack', '16/11/2026', 'Accord envoyé'], status: 'Acceptée', tone: 'green' },
        { cells: ['Sami Trabelsi', 'Data Analyst', '15/11/2026', 'Entretien le 22/11'], status: 'Entretien planifié', tone: 'amber' },
        { cells: ['Ines Kallel', 'UX/UI Designer', '14/11/2026', 'À étudier'], status: 'Présélectionnée', tone: 'blue' },
        { cells: ['Karim Slimani', 'Java Backend', '13/11/2026', 'En attente'], status: 'Reçue', tone: 'gray' },
        { cells: ['Nour Ayadi', 'Data Analyst', '11/11/2026', 'Clôturée'], status: 'Refusée', tone: 'red' }
      ]
    },
    profils: {
      title: 'Profils & CV',
      subtitle: 'Complétude des profils et badges obtenus',
      columns: ['Candidat', 'Compétences', 'Badges', 'Complétude', 'Mis à jour', 'Statut', 'Actions'],
      rows: [
        { cells: ['Lina Ben Ali', 'Angular, Java, SQL', '3', '92 %', '19/11/2026'], status: 'Complet', tone: 'green' },
        { cells: ['Sami Trabelsi', 'Python, Power BI', '2', '78 %', '18/11/2026'], status: 'En cours', tone: 'blue' },
        { cells: ['Ines Kallel', 'Figma, UX Research', '1', '61 %', '16/11/2026'], status: 'En cours', tone: 'blue' },
        { cells: ['Karim Slimani', 'Spring Boot, API REST', '0', '35 %', '12/11/2026'], status: 'Incomplet', tone: 'amber' }
      ]
    },
    entreprises: {
      title: 'Entreprises & Avis',
      subtitle: 'Annuaire, modération et évaluations',
      columns: ['Entreprise', 'Secteur', 'Ville', 'Note moyenne', 'Avis', 'Statut', 'Actions'],
      rows: [
        { cells: ['TechNova', 'Informatique', 'Tunis', '4,6 / 5', '48'], status: 'Vérifiée', tone: 'green' },
        { cells: ['DataPlus', 'Data & Analytics', 'Sfax', '4,2 / 5', '26'], status: 'Vérifiée', tone: 'green' },
        { cells: ['PixelLab', 'Design', 'Tunis', '4,8 / 5', '19'], status: 'Vérifiée', tone: 'green' },
        { cells: ['Nouvelle Entreprise', 'Conseil', 'Ariana', '-', '1'], status: 'À modérer', tone: 'amber' }
      ]
    },
    tests: {
      title: 'Tests & Compétences',
      subtitle: 'Correction automatique et tentatives',
      columns: ['Test', 'Compétence', 'Questions', 'Tentatives', 'Réussite', 'Statut', 'Actions'],
      rows: [
        { cells: ['Test Angular', 'Développement', '12', '3 max', '84 %'], status: 'Publié', tone: 'green' },
        { cells: ['Test SQL avancé', 'Data', '15', '3 max', '71 %'], status: 'Publié', tone: 'green' },
        { cells: ['Test Figma', 'Design', '10', '3 max', '90 %'], status: 'Brouillon', tone: 'gray' },
        { cells: ['Test Spring Boot', 'Développement', '18', '3 max', '66 %'], status: 'Publié', tone: 'green' }
      ]
    },
    alertes: {
      title: 'Alertes & Favoris',
      subtitle: 'Règles configurées par les candidats',
      columns: ['Alerte', 'Critères', 'Fréquence', 'Offres sauvegardées', 'Dernière notification', 'Statut', 'Actions'],
      rows: [
        { cells: ['Java Tunis', 'Java · Tunis · CDI', 'Quotidienne', '4', '18/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['Data Sfax', 'SQL · Sfax', 'Hebdomadaire', '2', '15/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['Design remote', 'Figma · Télétravail', 'Quotidienne', '0', '09/11/2026'], status: 'En pause', tone: 'amber' }
      ]
    },
    messagerie: {
      title: 'Messagerie',
      subtitle: 'Conversations entre candidats et recruteurs',
      columns: ['Conversation', 'Participants', 'Dernier message', 'Date', 'Non lus', 'Statut', 'Actions'],
      rows: [
        { cells: ['Lina / TechNova', '2', 'Merci pour votre retour', '19/11/2026', '0'], status: 'Active', tone: 'green' },
        { cells: ['Sami / Talent RH', '2', 'Entretien confirmé', '18/11/2026', '2'], status: 'Active', tone: 'green' },
        { cells: ['Ines / PixelLab', '2', 'À bientôt', '12/11/2026', '0'], status: 'Archivée', tone: 'gray' }
      ]
    },
    notifications: {
      title: 'Notifications envoyées',
      subtitle: 'Événements consommés par le service commun',
      columns: ['Type', 'Titre', 'Destinataire', 'Canal', 'Date', 'Statut', 'Actions'],
      rows: [
        { cells: ['StatusChanged', 'Candidature acceptée', 'Lina Ben Ali', 'Application', '19/11/2026'], status: 'Envoyée', tone: 'green' },
        { cells: ['TestPassed', 'Badge Angular', 'Sami Trabelsi', 'Application', '18/11/2026'], status: 'Envoyée', tone: 'green' },
        { cells: ['AlertMatched', 'Nouvelle offre Java', 'Karim Slimani', 'Email', '17/11/2026'], status: 'En attente', tone: 'amber' }
      ]
    },
    utilisateurs: {
      title: 'Utilisateurs & rôles',
      subtitle: 'Comptes gérés par le service commun (JWT)',
      columns: ['Utilisateur', 'Rôle', 'Email', 'Service', 'Dernière connexion', 'Statut', 'Actions'],
      rows: [
        { cells: ['Lina Ben Ali', 'CANDIDAT', 'lina@exemple.tn', 'Auth', '19/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['Sami Trabelsi', 'CANDIDAT', 'sami@exemple.tn', 'Auth', '18/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['TechNova RH', 'RECRUTEUR', 'rh@technova.tn', 'Auth', '17/11/2026'], status: 'Active', tone: 'green' },
        { cells: ['Admin SkillMatch', 'ADMIN', 'admin@skillmatch.tn', 'Auth', '16/11/2026'], status: 'Active', tone: 'green' }
      ]
    }
  };
  readonly activeSection = signal('offres');
  readonly sidebarOpen = signal(typeof window === 'undefined' ? true : window.innerWidth > 1180);
  readonly openNav = signal<string | null>(null);
  readonly notificationsOpen = signal(false);
  readonly unreadCount = signal(4);
  readonly navMessage = signal('');
  readonly query = signal('');
  readonly alertsEnabled = signal(true);
  readonly digestEnabled = signal(false);
  readonly moderationEnabled = signal(true);
  readonly users = signal<User[]>([]);
  readonly selectedUserId = signal<number | null>(null);
  readonly userNom = signal('');
  readonly userEmail = signal('');
  readonly userPassword = signal('');
  readonly userRole = signal<Role>('CANDIDAT');
  readonly userLoading = signal(false);
  readonly userSaving = signal(false);
  readonly userError = signal('');
  readonly pendingDelete = signal<User | null>(null);
  readonly deleteLoading = signal(false);
  private navMessageTimer?: number;
  readonly currentTable = computed(() => this.tables[this.activeSection()] ?? null);
  readonly currentLabel = computed(() => {
    const id = this.activeSection();
    if (id === this.dashboardItem.id) return this.dashboardItem.label;
    const entity = [...this.sideGroups, ...this.adminGroups].flatMap((group) => group.children).find((child) => child.id === id);
    return entity?.label ?? this.adminItems.find((item) => item.id === id)?.label ?? 'Tableau de bord';
  });
  readonly filteredRows = computed(() => {
    const q = this.query().trim().toLowerCase();
    const table = this.currentTable();
    if (!table) return [];
    return q ? table.rows.filter((row) => row.cells.some((cell) => cell.toLowerCase().includes(q))) : table.rows;
  });
  readonly filteredUsers = computed(() => {
    const q = this.query().trim().toLowerCase();
    const users = this.users();
    return q ? users.filter((user) => [user.nom, user.email, user.role].some((value) => value.toLowerCase().includes(q))) : users;
  });
  ngOnInit(): void { this.loadUsers(); }

  loadUsers(): void {
    this.userLoading.set(true);
    this.userError.set('');
    this.userService.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.tables['utilisateurs'] = {
          title: 'Utilisateurs & rôles',
          subtitle: 'Comptes gérés par le service commun (JWT)',
          columns: ['Utilisateur', 'Rôle', 'Email', 'Statut', 'Actions'],
          rows: users.map((user) => this.toUserRow(user))
        };
        this.userLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.userLoading.set(false);
        this.userError.set(this.readError(error));
        this.showMessage(`Impossible de charger les utilisateurs : ${this.userError()}`);
      }
    });
  }

  openUserForm(user?: User): void {
    this.selectedUserId.set(user?.id ?? null);
    this.userNom.set(user?.nom ?? '');
    this.userEmail.set(user?.email ?? '');
    this.userPassword.set('');
    this.userRole.set(user?.role ?? 'CANDIDAT');
    this.userError.set('');
  }

  resetUserForm(): void { this.openUserForm(); }

  setUserRole(value: string): void { this.userRole.set(value as Role); }

  saveUser(): void {
    const nom = this.userNom().trim();
    const email = this.userEmail().trim();
    const password = this.userPassword();
    const role = this.userRole();

    if (!nom || !email) {
      this.userError.set('Le nom et l’email sont obligatoires.');
      return;
    }
    if ((!this.selectedUserId() || password) && password.length < 6) {
      this.userError.set('Le mot de passe doit contenir au moins 6 caractères.');
      return;
    }

    this.userSaving.set(true);
    this.userError.set('');
    const id = this.selectedUserId();
    const request$ = id
      ? this.userService.update(id, { nom, email, role, ...(password ? { password } : {}) } as UpdateUserRequest)
      : this.userService.create({ nom, email, password, role } as CreateUserRequest);

    request$.subscribe({
      next: () => {
        this.userSaving.set(false);
        this.resetUserForm();
        this.loadUsers();
        this.showMessage(id ? 'Utilisateur mis à jour.' : 'Utilisateur créé.');
      },
      error: (error: HttpErrorResponse) => {
        this.userSaving.set(false);
        this.userError.set(this.readError(error));
      }
    });
  }

  editUser(user: User): void {
    this.openUserForm(user);
    this.selectSection('utilisateurs');
  }

  requestDelete(user: User): void {
    this.pendingDelete.set(user);
    this.userError.set('');
  }

  cancelDelete(): void {
    if (!this.deleteLoading()) {
      this.pendingDelete.set(null);
    }
  }

  confirmDelete(): void {
    const user = this.pendingDelete();
    if (!user || this.deleteLoading()) {
      return;
    }
    this.deleteLoading.set(true);
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.deleteLoading.set(false);
        this.pendingDelete.set(null);
        this.loadUsers();
        if (this.selectedUserId() === user.id) { this.resetUserForm(); }
        this.showMessage('Utilisateur supprimé.');
      },
      error: (error: HttpErrorResponse) => {
        this.deleteLoading.set(false);
        this.pendingDelete.set(null);
        this.showMessage(this.readError(error));
      }
    });
  }

  toggleSidebar(): void { this.sidebarOpen.update((open) => !open); }
  selectSection(id: string): void { this.activeSection.set(id);this.query.set('');this.closeNav(); }
  toggleNav(id: string): void { this.openNav.update((current) => current === id ? null : id); }
  isGroupActive(group: SideGroup): boolean { return group.children.some((child) => child.id === this.activeSection()) || this.openNav() === group.id; }
  closeNav(): void { this.openNav.set(null); }
  @HostListener('document:click')
  onDocumentClick(): void { this.closeNav();this.notificationsOpen.set(false); }
  @HostListener('document:keydown.escape')
  onEscape(): void { this.closeNav();this.notificationsOpen.set(false);this.cancelDelete(); }
  toggleNotifications(): void { this.notificationsOpen.update((open) => !open);this.closeNav(); }
  markAllRead(): void { this.unreadCount.set(0); }
  onNotification(notif: AppNotification): void {
    this.notificationsOpen.set(false);
    this.showMessage(`${notif.title} — ${notif.detail}`);
  }
  onNavAction(serviceId: string, label: string): void {
    this.closeNav();
    if (serviceId === 'offres') { this.selectSection(label.toLowerCase().includes('candidature') ? 'candidatures' : 'offres'); return; }
    const service = this.navServices.find((item) => item.id === serviceId);
    this.showMessage(`${service?.full ?? serviceId} · ${label} — module back office à brancher sur l'API.`);
  }
  onRowClick(row: AdminRow): void { this.showMessage(`${row.cells[0]} · ${row.status} — action de démonstration.`); }
  private toUserRow(user: User): AdminRow {
    return { cells: [user.nom, user.role, user.email], status: 'Actif', tone: 'green' };
  }
  private readError(error: HttpErrorResponse): string {
    return error.error?.message ?? error.error?.detail ?? error.message ?? 'Service utilisateur indisponible pour le moment.';
  }
  showMessage(text: string): void {
    this.navMessage.set(text);
    window.clearTimeout(this.navMessageTimer);
    this.navMessageTimer = window.setTimeout(() => this.navMessage.set(''), 5000);
  }
}
