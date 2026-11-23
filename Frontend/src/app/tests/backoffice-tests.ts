import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component, DestroyRef, ElementRef, Injector, OnInit, afterNextRender, computed, inject, input, output,
  signal, viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { TestDefinition, TestResult, TestSummary, TestsService, testErrorMessage } from './tests.service';

type QuestionDefinition = TestDefinition['questions'][number];
type QuestionForm = FormGroup<{
  id: FormControl<string>;
  enonce: FormControl<string>;
  options: FormArray<FormControl<string>>;
  indexBonneReponse: FormControl<number>;
}>;

@Component({
  selector: 'app-tests-backoffice',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './backoffice-tests.html',
  styleUrl: './backoffice-tests.css'
})
export class TestsBackoffice implements OnInit {
  private readonly api = inject(TestsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly editorPanel = viewChild<ElementRef<HTMLElement>>('editorPanel');
  private catalogueRequest?: Subscription;
  private definitionRequest?: Subscription;
  private resultsRequest?: Subscription;
  private questionNumber = 0;

  readonly view = input.required<'catalogue' | 'results'>();
  readonly showResults = output<void>();
  readonly catalogue = signal<TestSummary[]>([]);
  readonly query = signal('');
  readonly catalogueLoading = signal(false);
  readonly catalogueError = signal('');
  readonly feedback = signal('');

  readonly filtered = computed(() => {
    const query = this.query().trim().toLowerCase();
    return this.catalogue().filter(test =>
      test.titre.toLowerCase().includes(query) || test.competence.toLowerCase().includes(query));
  });

  readonly editorOpen = signal(false);
  readonly editedTest = signal<TestSummary | null>(null);
  readonly editorLoading = signal(false);
  readonly editorError = signal('');
  readonly saving = signal(false);
  readonly form = this.fb.group({
    titre: '',
    competence: '',
    dureeMinutes: 20,
    seuilReussite: 70,
    questions: this.fb.array<QuestionForm>([])
  });

  readonly pendingDelete = signal<TestSummary | null>(null);
  readonly deleting = signal(false);
  readonly deletionError = signal('');

  readonly selectedResultId = signal<number | null>(null);
  readonly resultRows = signal<TestResult[]>([]);
  readonly resultsLoading = signal(false);
  readonly resultsError = signal('');

  get questions(): FormArray<QuestionForm> {
    return this.form.controls.questions;
  }

  ngOnInit(): void {
    this.loadCatalogue();
  }

  loadCatalogue(): void {
    this.catalogueRequest?.unsubscribe();
    this.catalogueLoading.set(true);
    this.catalogueError.set('');
    this.catalogueRequest = this.api.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tests => {
        this.catalogue.set(tests);
        this.catalogueLoading.set(false);
        if (!tests.some(test => test.id === this.selectedResultId())) {
          this.selectResults(null);
        }
      },
      error: error => {
        this.catalogueLoading.set(false);
        this.catalogueError.set(testErrorMessage(error, 'Impossible de charger le catalogue de tests.'));
      }
    });
  }

  openEditor(test: TestSummary | null): void {
    if (this.saving() || this.deleting()) return;
    this.definitionRequest?.unsubscribe();
    this.feedback.set('');
    this.editorError.set('');
    this.editedTest.set(test);
    this.editorOpen.set(true);
    afterNextRender(() => this.editorPanel()?.nativeElement.focus(), { injector: this.injector });
    this.questions.clear();
    this.form.reset();
    if (!test) {
      this.editorLoading.set(false);
      this.addQuestion();
      return;
    }

    this.editorLoading.set(true);
    this.definitionRequest = this.api.definition(test.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: definition => {
        this.form.patchValue({
          titre: definition.titre,
          competence: definition.competence,
          dureeMinutes: definition.dureeMinutes,
          seuilReussite: definition.seuilReussite
        });
        for (const question of definition.questions) {
          this.questions.push(this.buildQuestion(question));
        }
        this.editorLoading.set(false);
      },
      error: error => {
        this.editorLoading.set(false);
        this.editorError.set(testErrorMessage(error, 'Impossible de charger la définition du test.'));
      }
    });
  }

  closeEditor(): void {
    if (this.saving()) return;
    this.definitionRequest?.unsubscribe();
    this.editorOpen.set(false);
    this.editorLoading.set(false);
    this.editorError.set('');
    this.editedTest.set(null);
    this.questions.clear();
    this.form.reset();
  }

  addQuestion(): void {
    if (this.questions.length >= 100) return;
    let id: string;
    do {
      id = `q${++this.questionNumber}`;
    } while (this.questions.controls.some(question => question.controls.id.value === id));
    this.questions.push(this.buildQuestion({
      id,
      enonce: '',
      options: ['', ''],
      indexBonneReponse: -1
    }));
  }

  removeQuestion(index: number): void {
    if (this.questions.length > 1) this.questions.removeAt(index);
  }

  addOption(question: QuestionForm): void {
    if (question.controls.options.length < 8) {
      question.controls.options.push(this.fb.control(''));
    }
  }

  removeOption(question: QuestionForm, index: number): void {
    if (question.controls.options.length <= 2) return;
    question.controls.options.removeAt(index);
    const correct = question.controls.indexBonneReponse.value;
    if (correct === index) question.controls.indexBonneReponse.setValue(-1);
    if (correct > index) question.controls.indexBonneReponse.setValue(correct - 1);
  }

  save(): void {
    if (this.saving() || this.editorLoading() || this.deleting()) return;
    const values = this.form.getRawValue();
    const definition: TestDefinition = {
      ...values,
      titre: values.titre.trim(),
      competence: values.competence.trim(),
      questions: values.questions.map(question => ({
        ...question,
        enonce: question.enonce.trim(),
        options: question.options.map(option => option.trim())
      }))
    };
    const validationError = this.validateDefinition(definition);
    this.editorError.set(validationError);
    if (validationError) return;

    const test = this.editedTest();
    const request = test ? this.api.update(test.id, definition) : this.api.create(definition);
    this.saving.set(true);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeEditor();
        this.feedback.set(test ? 'Test mis à jour.' : 'Test créé.');
        this.loadCatalogue();
      },
      error: error => {
        this.saving.set(false);
        this.editorError.set(this.mutationError(error, 'Impossible d’enregistrer le test.'));
      }
    });
  }

  requestDelete(test: TestSummary): void {
    if (this.saving() || this.deleting()) return;
    this.feedback.set('');
    this.deletionError.set('');
    this.pendingDelete.set(test);
  }

  cancelDelete(): void {
    if (this.deleting()) return;
    this.pendingDelete.set(null);
    this.deletionError.set('');
  }

  confirmDelete(): void {
    const test = this.pendingDelete();
    if (!test || this.deleting() || this.saving()) return;
    this.deleting.set(true);
    this.deletionError.set('');
    this.api.remove(test.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.cancelDelete();
        if (this.editedTest()?.id === test.id) this.closeEditor();
        if (this.selectedResultId() === test.id) this.selectResults(null);
        this.feedback.set('Test supprimé.');
        this.loadCatalogue();
      },
      error: error => {
        this.deleting.set(false);
        this.deletionError.set(this.mutationError(error, 'Impossible de supprimer le test.'));
      }
    });
  }

  openResults(id: number): void {
    this.selectResults(id);
    this.showResults.emit();
  }

  selectResults(id: number | null): void {
    this.resultsRequest?.unsubscribe();
    this.selectedResultId.set(id);
    this.resultRows.set([]);
    this.resultsError.set('');
    this.resultsLoading.set(id !== null);
    if (id === null) return;

    this.resultsRequest = this.api.results(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: results => {
        this.resultRows.set(results);
        this.resultsLoading.set(false);
      },
      error: error => {
        this.resultsLoading.set(false);
        this.resultsError.set(testErrorMessage(error, 'Impossible de charger les résultats.'));
      }
    });
  }

  private buildQuestion(question: QuestionDefinition): QuestionForm {
    return this.fb.group({
      id: question.id,
      enonce: question.enonce,
      options: this.fb.array(question.options.map(option => this.fb.control(option))),
      indexBonneReponse: question.indexBonneReponse
    });
  }

  private validateDefinition(definition: TestDefinition): string {
    if (!definition.titre || !definition.competence) return 'Renseignez le titre et la compétence.';
    if (!Number.isInteger(definition.dureeMinutes) || definition.dureeMinutes < 1 || definition.dureeMinutes > 240) {
      return 'La durée doit être comprise entre 1 et 240 minutes.';
    }
    if (!Number.isInteger(definition.seuilReussite) || definition.seuilReussite < 1 || definition.seuilReussite > 100) {
      return 'Le seuil de réussite doit être compris entre 1 et 100 %.';
    }
    if (!definition.questions.length) return 'Ajoutez au moins une question.';
    for (const [index, question] of definition.questions.entries()) {
      const position = index + 1;
      if (!question.enonce || question.options.some(option => !option)) {
        return `Complétez l’énoncé et les options de la question ${position}.`;
      }
      if (new Set(question.options).size !== question.options.length) {
        return `Les options de la question ${position} doivent être distinctes.`;
      }
      if (question.indexBonneReponse < 0 || question.indexBonneReponse >= question.options.length) {
        return `Choisissez la bonne réponse de la question ${position}.`;
      }
    }
    return '';
  }

  private mutationError(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Ce test est verrouillé car il comporte déjà des résultats.';
    }
    return testErrorMessage(error, fallback);
  }
}
