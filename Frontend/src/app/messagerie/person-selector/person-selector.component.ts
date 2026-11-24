import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  effect,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, catchError, debounce, forkJoin, map, of, switchMap, tap, timer } from 'rxjs';
import { AuthService } from '../../user/auth.service';
import {
  PucePalette,
  UserSummary,
  couleurPourUtilisateur,
  extrairePrenom,
  formaterInitiales,
  formaterNomCourt,
  formaterRoleLibelle,
  normaliserTexte
} from '../messagerie.models';
import { MessagerieService } from '../messagerie.service';

const DEBOUNCE_MS = 300;
const MAX_SUGGESTIONS = 10;
let nextSelectorUid = 1;

interface SearchTrigger {
  terme: string;
  immediate: boolean;
  seq: number;
}

@Component({
  selector: 'app-person-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './person-selector.component.html',
  styleUrl: './person-selector.component.scss'
})
export class PersonSelectorComponent implements OnInit, OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly api = inject(MessagerieService);
  private readonly auth = inject(AuthService);

  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;

  readonly uid = nextSelectorUid++;
  readonly listboxId = `ps-suggestions-${this.uid}`;

  // Inputs
  readonly participants = input<UserSummary[]>([]);
  readonly exclureIds = input<number[]>([]);
  readonly contactsRecents = input<UserSummary[]>([]);
  readonly hasConversationIds = input<number[]>([]);
  readonly verrouillerCreateur = input<boolean>(false);
  readonly idCreateur = input<number | null>(null);
  readonly multiple = input<boolean>(true);
  readonly roleFilter = input<'ALL' | 'CANDIDAT' | 'RECRUTEUR'>('ALL');
  readonly placeholder = input<string>('Rechercher par nom…');
  readonly ariaLabel = input<string>('Rechercher des personnes par nom');

  // Outputs
  readonly participantsChange = output<UserSummary[]>();
  readonly erreur = output<string>();

  // État interne
  readonly query = signal('');
  readonly suggestions = signal<UserSummary[]>([]);
  readonly charging = signal(false);
  readonly erreurReseau = signal('');
  readonly actifIndex = signal(-1);
  readonly ouvert = signal(false);
  readonly indiceDeuxCaracteres = signal(false);

  readonly activeOptionId = computed(() => {
    const idx = this.actifIndex();
    const items = this.suggestions();
    if (!this.ouvert() || idx < 0 || idx >= items.length) return null;
    return `ps-opt-${this.uid}-${idx}`;
  });

  private readonly cacheUtilisateurs = new Map<number, UserSummary>();
  private readonly searchTrigger$ = new Subject<SearchTrigger>();
  private searchSub?: Subscription;
  private searchSeq = 0;
  private derniereRecherche = '';

  constructor() {
    effect(() => {
      for (const c of this.contactsRecents()) {
        this.memoriserUtilisateur(c);
      }
      for (const p of this.participants()) {
        this.memoriserUtilisateur(p);
      }
    });
  }

  ngOnInit(): void {
    this.searchSub = this.searchTrigger$
      .pipe(
        debounce((req) => (req.immediate ? timer(0) : timer(DEBOUNCE_MS))),
        tap((req) => {
          this.erreurReseau.set('');
          this.indiceDeuxCaracteres.set(false);
          if (req.terme.length >= 2) {
            this.charging.set(true);
            this.ouvert.set(true);
          }
        }),
        switchMap((req) => this.executerRecherche(req.terme))
      )
      .subscribe((result) => {
        if (result.error) {
          this.charging.set(false);
          this.suggestions.set([]);
          this.erreurReseau.set(result.error);
          this.ouvert.set(true);
          this.erreur.emit(result.error);
          return;
        }
        this.charging.set(false);
        this.suggestions.set(result.users);
        this.actifIndex.set(result.users.length > 0 ? 0 : -1);
        this.indiceDeuxCaracteres.set(result.terme.length === 1);
        if (result.terme.length === 0) {
          this.ouvert.set(result.users.length > 0);
        } else {
          this.ouvert.set(true);
        }
      });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.el.nativeElement.contains(event.target as Node)) {
      this.ouvert.set(false);
    }
  }

  focusInput(): void {
    this.searchInput?.nativeElement.focus();
  }

  onFocus(): void {
    if (!this.query().trim()) {
      this.declencherRecherche('', true);
    } else if (this.suggestions().length > 0 || this.erreurReseau()) {
      this.ouvert.set(true);
    }
  }

  onQueryChange(value: string): void {
    this.query.set(value);
    const terme = value.trim();
    this.derniereRecherche = terme;
    if (terme.length >= 2) {
      this.charging.set(true);
      this.ouvert.set(true);
    }
    this.declencherRecherche(terme, terme.length < 2);
  }

  onRoleFilterReload(): void {
    const terme = this.query().trim();
    this.declencherRecherche(terme, true);
  }

  reessayer(): void {
    this.erreurReseau.set('');
    const terme = this.derniereRecherche || this.query().trim();
    this.declencherRecherche(terme, true);
  }

  effacerRecherche(): void {
    this.query.set('');
    this.derniereRecherche = '';
    this.erreurReseau.set('');
    this.declencherRecherche('', true);
    this.focusInput();
  }

  selectionner(user: UserSummary): void {
    this.memoriserUtilisateur(user);
    this.ajouter(user);
    this.query.set('');
    this.derniereRecherche = '';
    this.suggestions.set([]);
    this.actifIndex.set(-1);
    this.ouvert.set(false);
    if (this.multiple()) {
      window.setTimeout(() => this.focusInput(), 0);
    }
  }

  ajouter(user: UserSummary): void {
    if (this.estAdminBloque(user)) {
      this.erreur.emit('Vous ne pouvez pas écrire à un administrateur.');
      return;
    }
    if (!this.multiple()) {
      this.participantsChange.emit([user]);
      return;
    }
    if (this.participants().some((p) => p.id === user.id)) {
      return;
    }
    this.participantsChange.emit([...this.participants(), user]);
  }

  retirer(id: number): void {
    if (this.estCreateurVerrouille(id)) {
      return;
    }
    this.participantsChange.emit(this.participants().filter((p) => p.id !== id));
    window.setTimeout(() => this.focusInput(), 0);
  }

  onKeydown(event: KeyboardEvent): void {
    const items = this.suggestions();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!this.ouvert()) {
        this.declencherRecherche(this.query().trim(), true);
        return;
      }
      if (!items.length) return;
      this.actifIndex.update((i) => (i < 0 ? 0 : (i + 1) % items.length));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (!items.length) return;
      this.actifIndex.update((i) => (i <= 0 ? items.length - 1 : i - 1));
    } else if (event.key === 'Enter') {
      if (this.ouvert() && items.length > 0) {
        event.preventDefault();
        const idx = this.actifIndex() >= 0 && this.actifIndex() < items.length ? this.actifIndex() : 0;
        const user = items[idx];
        if (user) {
          this.selectionner(user);
        }
      }
    } else if (event.key === 'Backspace') {
      if (!this.query() && this.participants().length > 0) {
        const dernier = this.participants()[this.participants().length - 1];
        if (dernier && !this.estCreateurVerrouille(dernier.id)) {
          event.preventDefault();
          this.retirer(dernier.id);
        }
      }
    } else if (event.key === 'Escape') {
      if (this.ouvert()) {
        event.stopPropagation();
        this.ouvert.set(false);
      }
    }
  }

  estCreateurVerrouille(id: number): boolean {
    return this.verrouillerCreateur() && this.idCreateur() !== null && id === this.idCreateur();
  }

  libellePuce(user: UserSummary): string {
    if (this.estCreateurVerrouille(user.id)) {
      return 'Vous';
    }
    return extrairePrenom(user.nom);
  }

  couleurPuce(id: number): PucePalette {
    return couleurPourUtilisateur(id);
  }

  initiales(nom: string): string {
    return formaterInitiales(nom);
  }

  nomCourt(nom: string): string {
    return formaterNomCourt(nom);
  }

  libelleRole(role: string | null): string {
    return formaterRoleLibelle(role);
  }

  aDejaConversation(id: number): boolean {
    return this.hasConversationIds().includes(id);
  }

  private declencherRecherche(terme: string, immediate: boolean): void {
    this.searchTrigger$.next({ terme, immediate, seq: ++this.searchSeq });
  }

  private executerRecherche(terme: string) {
    if (terme.length === 0) {
      return of({ terme, users: this.filtrerLocalement(''), error: '' });
    }
    if (terme.length === 1) {
      return of({ terme, users: this.filtrerLocalement(terme), error: '' });
    }

    const isAdmin = this.auth.isAdmin();
    const roleSel = this.roleFilter();

    const requete$ = !isAdmin && roleSel === 'ALL'
      ? forkJoin([
          this.api.searchDirectory(terme, 'CANDIDAT'),
          this.api.searchDirectory(terme, 'RECRUTEUR')
        ]).pipe(map(([candidats, recruteurs]) => [...(candidats ?? []), ...(recruteurs ?? [])]))
      : this.api.searchDirectory(terme, roleSel === 'ALL' ? null : roleSel);

    return requete$.pipe(
      map((users) => {
        const dedup = new Map<number, UserSummary>();
        for (const u of users ?? []) {
          if (!u || this.estAdminBloque(u)) continue;
          this.memoriserUtilisateur(u);
          dedup.set(u.id, u);
        }
        const filtered = this.appliquerExclusionsEtTri(Array.from(dedup.values()));
        return { terme, users: filtered.slice(0, MAX_SUGGESTIONS), error: '' };
      }),
      catchError(() =>
        of({
          terme,
          users: [] as UserSummary[],
          error: "Impossible de joindre l'annuaire."
        })
      )
    );
  }

  private filtrerLocalement(terme: string): UserSummary[] {
    const normTerme = normaliserTexte(terme);
    const pool = new Map<number, UserSummary>();

    for (const c of this.contactsRecents()) {
      if (c && !this.estAdminBloque(c)) {
        pool.set(c.id, c);
      }
    }
    if (normTerme.length > 0) {
      for (const u of this.cacheUtilisateurs.values()) {
        if (u && !this.estAdminBloque(u) && !pool.has(u.id)) {
          pool.set(u.id, u);
        }
      }
    }

    const matches = Array.from(pool.values()).filter((u) => {
      if (!normTerme) return true;
      return normaliserTexte(u.nom).includes(normTerme);
    });

    return this.appliquerExclusionsEtTri(matches, normTerme.length === 0).slice(0, MAX_SUGGESTIONS);
  }

  private appliquerExclusionsEtTri(users: UserSummary[], conserverOrdre = false): UserSummary[] {
    const currentId = this.auth.currentUser()?.id;
    const exclus = new Set(this.exclureIds());
    const dejaSelectionnes = new Set(this.participants().map((p) => p.id));
    const roleSel = this.roleFilter();

    const filtered = users.filter((u) => {
      if (!u || u.id === currentId) return false;
      if (exclus.has(u.id) || dejaSelectionnes.has(u.id)) return false;
      if (this.estAdminBloque(u)) return false;
      if (roleSel !== 'ALL' && String(u.role ?? '').toUpperCase() !== roleSel) return false;
      return true;
    });

    if (conserverOrdre) {
      return filtered;
    }
    return filtered.sort((a, b) => normaliserTexte(a.nom).localeCompare(normaliserTexte(b.nom)));
  }

  private estAdminBloque(user: UserSummary): boolean {
    if (this.auth.isAdmin()) return false;
    return String(user.role ?? '').toUpperCase() === 'ADMIN';
  }

  private memoriserUtilisateur(user: UserSummary): void {
    if (!user || !user.id || !user.nom) return;
    if (this.estAdminBloque(user)) return;
    this.cacheUtilisateurs.set(user.id, user);
  }
}
