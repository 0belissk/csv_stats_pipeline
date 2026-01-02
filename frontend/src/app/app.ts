import { Component, signal } from '@angular/core';
import { LoginComponent } from './login/login';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LoginComponent],
  template: `<app-login></app-login>`
})
export class AppComponent {
  constructor() {
    console.log('APP COMPONENT LOADED');
  }
}