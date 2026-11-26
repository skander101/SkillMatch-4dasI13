import { Component, input, output } from '@angular/core';

export type ConfirmTone = 'danger' | 'warning' | 'primary';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.css'
})
export class ConfirmDialog {
  readonly title = input('Confirmer cette action ?');
  readonly message = input('');
  readonly confirmLabel = input('Confirmer');
  readonly cancelLabel = input('Annuler');
  readonly loading = input(false);
  readonly tone = input<ConfirmTone>('danger');
  readonly cancel = output<void>();
  readonly confirm = output<void>();
}
