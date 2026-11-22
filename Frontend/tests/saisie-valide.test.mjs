import '@angular/compiler';
import { ElementRef, Injector, runInInjectionContext } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { readFileSync, mkdirSync, writeFileSync, unlinkSync } from 'node:fs';
import { after, test } from 'node:test';
import assert from 'node:assert/strict';
import ts from 'typescript';

// Compile uniquement le petit validateur ; aucune dépendance de test supplémentaire.
mkdirSync('.angular', { recursive: true });
const fichier = '.angular/saisie-valide-test.mjs';
const source = readFileSync('src/app/saisie-valide.ts', 'utf8');
writeFileSync(fichier, ts.transpileModule(source, { compilerOptions: {
  experimentalDecorators: true, target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022
} }).outputText);
const { SaisieValide } = await import('../' + fichier);
const champ = { validity: { badInput: false } };
const injector = Injector.create({ providers: [{ provide: ElementRef, useValue: new ElementRef(champ) }] });
const validateur = runInInjectionContext(injector, () => new SaisieValide());
after(() => { injector.destroy(); unlinkSync(fichier); });

function valide(valeur, type, regles = []) {
  validateur.saisieValide = type;
  return Validators.compose([c => validateur.validate(c), ...regles])(new FormControl(valeur)) === null;
}

test('Identifiants numériques : entiers positifs', () => {
  const regles = [Validators.required, Validators.min(1)];
  assert.ok(valide(1, 'entier', regles));
  for (const valeur of [null, 0, -1, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) assert.equal(valide(valeur, 'entier', regles), false);
});

test('salaire : obligatoire, décimal positif ou nul', () => {
  for (const valeur of [0, 1500.5]) assert.ok(valide(valeur, 'decimal', [Validators.required, Validators.min(0)]));
  assert.equal(valide(null, 'decimal', [Validators.required, Validators.min(0)]), false);
  for (const valeur of [-1, NaN, Infinity]) assert.equal(valide(valeur, 'decimal', [Validators.min(0)]), false);
});

test('note : entier positif ou nul dans la limite backend', () => {
  const regles = [Validators.min(0), Validators.max(2147483647)];
  assert.ok(valide(0, 'entier', regles));
  for (const valeur of [-1, 1.5, 2147483648]) assert.equal(valide(valeur, 'entier', regles), false);
});

test('date : valeur valide, champ facultatif vide ou obligatoire', () => {
  assert.ok(valide('2027-03-15T10:30', 'date'));
  assert.ok(valide('', 'date'));
  assert.equal(valide('', 'date', [Validators.required]), false);
  assert.equal(valide('date incorrecte', 'date'), false);
});

test('une saisie navigateur incorrecte reste invalide même avec un modèle vide', () => {
  champ.validity.badInput = true;
  assert.equal(valide(null, 'decimal'), false);
  assert.equal(valide('', 'date'), false);
  champ.validity.badInput = false;
});

// Les mêmes règles de texte sont utilisées pour ADMIN, CANDIDAT et RECRUTEUR.
for (const [nom, min, max] of [['entreprise', 2, 255], ['titre', 3, 255], ['description', 20, 10000], ['lettre', 20, 10000], ['commentaire', 5, 10000]]) {
  test(nom + ' : obligatoire, minimum et maximum après nettoyage des espaces', () => {
    champ.minLength = min; champ.maxLength = max;
    const regles = [Validators.required];
    for (const texte of ['', ' '.repeat(min), 'a'.repeat(min - 1), 'a'.repeat(max + 1), ' '.repeat(min) + 'a']) {
      assert.equal(valide(texte, 'texte', regles), false);
    }
    assert.ok(valide('a'.repeat(min), 'texte', regles));
    assert.ok(valide('a'.repeat(max), 'texte', regles));
    assert.ok(valide('  ' + 'a'.repeat(min) + '  ', 'texte', regles));
  });
}

test('compétences : aucune entrée vide, 255 caractères maximum chacune', () => {
  assert.ok(valide('Java, Spring Boot', 'competences', [Validators.required]));
  for (const texte of ['', ' ', ',,', 'Java, ', 'a'.repeat(256)]) assert.equal(valide(texte, 'competences', [Validators.required]), false);
});
