import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  isSubmitting = false;
  errorMessage: string | null = null;

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard', 'uploads']);
    }
  }

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.errorMessage = 'Email and password are required.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    this.authService
      .login(this.email, this.password)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => this.router.navigate(['/dashboard', 'uploads']),
        error: err => {
          this.errorMessage = err?.error?.error ?? 'Unable to sign in. Please try again.';
        }
      });
  }
}
