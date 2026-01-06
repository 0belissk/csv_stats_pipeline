import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth';                

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h2>Login</h2>

<input
  placeholder="Email"
  [(ngModel)]="email"
/>

<input
  placeholder="Password"
  type="password"
  [(ngModel)]="password"
/>

<button type="button" (click)="login()">Login</button>

  `
})
export class Login {
  email = '';
  password = '';

  constructor(private authService: AuthService) {}

  login() {
    console.log('LOGIN CLICKED', this.email, this.password);

    this.authService.login(this.email, this.password)
      .subscribe({
        next: res => {
          console.log('LOGIN SUCCESS', res);
          this.authService.storeToken(res.token);

          // 🔑 Call protected endpoint
          this.authService.getMe().subscribe(me => {
            console.log('ME endpoint response:', me);
          });
        },
        error: err => {
          console.error('LOGIN FAILED', err);
        }
      });
  }


}
