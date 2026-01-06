import { Component, signal } from '@angular/core';
import { Login } from './login/login';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [Login],
  template: `
    <h1>Hello, frontend</h1>
    <app-login></app-login>
  `
})
export class App {
  constructor() {
    console.log('APP COMPONENT LOADED');
  }
}
