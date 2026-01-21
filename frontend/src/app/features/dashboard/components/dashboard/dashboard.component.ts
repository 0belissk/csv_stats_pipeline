import { CommonModule } from '@angular/common';
import { Component, Signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  readonly userEmail: Signal<string | null>;

  constructor(private readonly authService: AuthService, private readonly router: Router) {
    this.userEmail = this.authService.currentEmail;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
