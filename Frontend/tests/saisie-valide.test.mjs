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

test('CV : entier positif obligatoire, jamais décimal ou vide', () => {
  const regles = [Validators.required, Validators.min(1)];
  assert.ok(valide(1, 'entier', regles));
  for (const valeur of [null, 0, -1, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) assert.equal(valide(valeur, 'entier', regles), false);
});

test('salaire : facultatif, décimal positif ou nul', () => {
  for (const valeur of [null, 0, 1500.5]) assert.ok(valide(valeur, 'decimal', [Validators.min(0)]));
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
