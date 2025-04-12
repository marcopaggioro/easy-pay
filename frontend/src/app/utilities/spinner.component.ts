import {Component, ElementRef, ViewChild} from '@angular/core';

@Component({
  selector: 'app-spinner',
  template: `
    <div #loading class="d-flex justify-content-center align-items-center">
      <div class="spinner-border" role="status"></div>
    </div>`
})
export class SpinnerComponent {
  @ViewChild('loading') loading!: ElementRef;

  show() {
    this.loading.nativeElement.classList.remove('d-none');
  }

  hide() {
    this.loading.nativeElement.classList.add('d-none');
  }
}
