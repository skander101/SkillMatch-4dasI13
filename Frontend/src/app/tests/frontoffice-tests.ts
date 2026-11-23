import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, effect, ElementRef, inject, input, output, signal, untracked, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, Observable, of, Subscription, tap, throwError } from 'rxjs';
import { AuthService } from '../user/auth.service';
import { TestDetail, TestResult, TestsService, TestSummary, testErrorMessage } from './tests.service';

export type FrontofficeTestsView = 'catalogue' | 'resultats' | 'verification';

type Chargement<T> =
  | { state: 'idle' | 'loading' }
  | { state: 'ready'; value: T }
  | { state: 'error'; message: string };

interface Progression {
  tentatives: number;
  reussi: boolean;
}

@Component({
  selector: 'app-tests-frontoffice',
  imports: [DatePipe, FormsModule],
  templateUrl: './frontoffice-tests.html',
  styleUrl: './frontoffice-tests.css'
})
export class FrontofficeTests {
  private readonly api = inject(TestsService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly vue = input.required<FrontofficeTestsView>();
  readonly vueChange = output<FrontofficeTestsView>();
  readonly connexion = output<void>();

  readonly role = computed(() => this.auth.isLoggedIn() ? this.auth.currentUser()?.role : null);
  readonly vueActive = computed(() => {
    const vue = this.vue();
    const role = this.role();
    if (vue === 'resultats' && role !== 'CANDIDAT') return 'catalogue';
    if (vue === 'verification' && role !== 'RECRUTEUR' && role !== 'ADMIN') return 'catalogue';
    return vue;
  });

  readonly competence = signal('');
  readonly filtreApplique = signal('');
  readonly catalogue = signal<Chargement<TestSummary[]>>({ state: 'idle' });
  readonly detail = signal<Chargement<TestDetail>>({ state: 'idle' });
  readonly historique = signal<Chargement<TestResult[]>>({ state: 'idle' });
  readonly envoi = signal<Chargement<TestResult>>({ state: 'idle' });
  readonly verification = signal<Chargement<TestResult | null>>({ state: 'idle' });
  readonly candidatAVerifier = signal<number | null>(null);
  readonly testAVerifier = signal<number | null>(null);
  readonly reponses = signal<Record<string, number>>({});
  readonly nombreReponses = computed(() => Object.keys(this.reponses()).length);
  readonly progression = computed(() => {
    const historique = this.historique();
    if (historique.state !== 'ready') return null;
    const tests = new Map<number, Progression>();
    for (const resultat of historique.value) {
      const courant = tests.get(resultat.testId);
      tests.set(resultat.testId, {
        tentatives: Math.max(courant?.tentatives ?? 0, resultat.tentative),
        reussi: (courant?.reussi ?? false) || resultat.reussi
      });
    }
    return tests;
  });
  readonly nombreReussites = computed(() => {
    const progression = this.progression();
    return progression ? [...progression.values()].filter(test => test.reussi).length : null;
  });
  readonly nomsTests = signal<ReadonlyMap<number, string>>(new Map());
  private requeteCatalogue?: Subscription;
  private requeteDetail?: Subscription;
  private requeteHistorique?: Subscription;
  private tentativeAConfirmer: { testId: number; tentativesConnues: number } | null = null;

  constructor() {
    effect(() => {
      const role = this.role();
      const vue = this.vueActive();
      untracked(() => {
        if (!role) return;
        if (this.catalogue().state === 'idle') this.chargerCatalogue();
        if (role === 'CANDIDAT' && this.historique().state === 'idle') this.chargerHistorique();
        if (vue === 'verification' && this.filtreApplique()) this.effacerFiltre();
      });
    });
  }

  ouvrirVue(vue: FrontofficeTestsView): void {
    if (vue === 'catalogue') this.fermerDetail();
    this.vueChange.emit(vue);
    this.host.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  chargerCatalogue(): void {
    this.requeteCatalogue?.unsubscribe();
    const filtre = this.competence().trim();
    this.filtreApplique.set(filtre);
    const catalogue = this.api.list(filtre || undefined).pipe(tap(tests => {
      this.nomsTests.update(nomsConnus => {
        const noms = new Map(nomsConnus);
        for (const test of tests) noms.set(test.id, test.titre);
        return noms;
      });
    }));
    this.requeteCatalogue = this.charger(catalogue, this.catalogue, 'Le catalogue des tests est indisponible.');
  }

  effacerFiltre(): void {
    this.competence.set('');
    this.chargerCatalogue();
  }

  ouvrirTest(id: number): void {
    this.requeteDetail?.unsubscribe();
    this.tentativeAConfirmer = null;
    this.reponses.set({});
    this.envoi.set({ state: 'idle' });
    this.requeteDetail = this.charger(this.api.detail(id), this.detail, 'Ce test est indisponible.');
    this.host.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  fermerDetail(): void {
    if (this.envoi().state === 'loading') return;
    this.requeteDetail?.unsubscribe();
    this.tentativeAConfirmer = null;
    this.detail.set({ state: 'idle' });
    this.reponses.set({});
    this.envoi.set({ state: 'idle' });
  }

  chargerHistorique(): void {
    this.requeteHistorique?.unsubscribe();
    const historique = this.api.myResults().pipe(tap(resultats => {
      const tentative = this.tentativeAConfirmer;
      if (!tentative) return;
      this.tentativeAConfirmer = null;
      const resultat = resultats
        .filter(test => test.testId === tentative.testId && test.tentative > tentative.tentativesConnues)
        .sort((a, b) => b.tentative - a.tentative)[0];
      if (resultat) {
        this.envoi.set({ state: 'ready', value: resultat });
        this.reponses.set({});
      }
    }));
    this.requeteHistorique = this.charger(
      historique, this.historique, 'Vos résultats sont indisponibles.'
    );
  }

  tentativesRestantes(testId: number): number | null {
    const progression = this.progression();
    return progression ? Math.max(0, 3 - (progression.get(testId)?.tentatives ?? 0)) : null;
  }

  choisir(questionId: string, index: number): void {
    this.reponses.update(reponses => ({ ...reponses, [questionId]: index }));
  }

  soumettre(testId: number): void {
    const historique = this.historique();
    if (this.role() !== 'CANDIDAT' || historique.state !== 'ready' || !this.nombreReponses() || this.envoi().state === 'loading'
      || this.progression()?.get(testId)?.reussi || this.tentativesRestantes(testId) === 0) {
      return;
    }
    const tentativesConnues = this.progression()?.get(testId)?.tentatives ?? 0;
    this.envoi.set({ state: 'loading' });
    this.api.submit(testId, this.reponses()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: resultat => {
        const resultats = historique.value.filter(test => test.id !== resultat.id);
        this.historique.set({ state: 'ready', value: [resultat, ...resultats] });
        this.envoi.set({ state: 'ready', value: resultat });
        this.reponses.set({});
      },
      error: (erreur: unknown) => {
        this.envoi.set({ state: 'error', message: testErrorMessage(erreur, 'L’envoi des réponses a échoué.') });
        if (erreur instanceof HttpErrorResponse && (erreur.status === 0 || erreur.status === 409 || erreur.status >= 500)) {
          this.tentativeAConfirmer = { testId, tentativesConnues };
          this.chargerHistorique();
        }
      }
    });
  }

  recommencer(): void {
    this.reponses.set({});
    this.envoi.set({ state: 'idle' });
  }

  readonly peutVerifier = computed(() => {
    const candidat = this.candidatAVerifier();
    const test = this.testAVerifier();
    return candidat !== null && Number.isSafeInteger(candidat) && candidat > 0
      && test !== null && Number.isSafeInteger(test) && test > 0;
  });

  verifier(): void {
    const candidat = this.candidatAVerifier();
    const test = this.testAVerifier();
    if (!this.peutVerifier() || candidat === null || test === null || this.verification().state === 'loading') return;
    this.charger(
      this.api.check(candidat, test).pipe(catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404
          && error.error?.detail === 'Aucun résultat réussi.') {
          return of(null);
        }
        return throwError(() => error);
      })),
      this.verification,
      'La vérification de la compétence a échoué.'
    );
  }

  private charger<T>(requete: Observable<T>, destination: WritableSignal<Chargement<T>>, message: string): Subscription {
    destination.set({ state: 'loading' });
    return requete.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: valeur => destination.set({ state: 'ready', value: valeur }),
      error: (erreur: unknown) => destination.set({ state: 'error', message: testErrorMessage(erreur, message) })
    });
  }
}
