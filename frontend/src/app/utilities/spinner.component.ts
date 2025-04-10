import {Component, ElementRef, ViewChild} from '@angular/core';

@Component({
  selector: 'app-spinner',
  template: `
    <div #loading class="h-100 w-100 position-fixed d-flex justify-content-center align-items-center text-light top-0 start-0"
         style="background-color: rgba(0, 0, 0, 0.5); z-index: 1">
      <div class="spinner-border" role="status"></div>
    </div>`
})
export class SpinnerComponent {
  @ViewChild('loading') loading!: ElementRef;

  show() {
    this.loading.nativeElement.classList.remove('invisible');
  }

  hide() {
    this.loading.nativeElement.classList.add('invisible');
  }
}
