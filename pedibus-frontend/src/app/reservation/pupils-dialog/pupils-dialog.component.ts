import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';


@Component({
    selector: 'app-pupils-dialog',
    templateUrl: 'pupils-dialog.component.html',
})
export class PupilsDialogComponent {
    constructor(
        public dialogRef: MatDialogRef<PupilsDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public pupils
    ) { }
}