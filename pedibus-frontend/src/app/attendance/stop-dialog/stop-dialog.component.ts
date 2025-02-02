import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material';


@Component({
    selector: 'app-stop-dialog',
    templateUrl: 'stop-dialog.component.html',
})
export class StopDialogComponent implements OnInit {
    selectedStop;

    constructor(@Inject(MAT_DIALOG_DATA) public data) {}

    ngOnInit(): void {
        this.selectedStop = this.data.stops[0];
    }
}