import { Directive, ElementRef, HostListener, Input, forwardRef, inject } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator } from '@angular/forms';

@Directive({
  selector: 'input[saisieValide]', standalone: true,
  providers: [{ provide: NG_VALIDATORS, useExisting: forwardRef(() => SaisieValide), multi: true }]
})
export class SaisieValide implements Validator {
  @Input() saisieValide: 'entier' | 'decimal' | 'date' = 'entier';
  private readonly champ = inject(ElementRef<HTMLInputElement>);
  private changement = () => {};

  validate(control: AbstractControl) { // Une saisie « e » ne doit pas devenir un champ facultatif vide.
    if (this.champ.nativeElement.validity.badInput) return { saisie: true };
    if (control.value === null || control.value === '') return null; // required gère les champs obligatoires.
    if (this.saisieValide === 'date') return Number.isFinite(Date.parse(control.value)) ? null : { saisie: true };
    const valide = this.saisieValide === 'entier' ? Number.isSafeInteger(control.value) : Number.isFinite(control.value);
    return valide ? null : { saisie: true };
  }

  registerOnValidatorChange(fn: () => void): void { this.changement = fn; }

  @HostListener('input')
  verifier(): void { this.changement(); }
}
