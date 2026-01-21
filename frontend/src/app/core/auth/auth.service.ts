import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api/api.config';

export interface AuthResponse {
  userId: number;
  email: string;
  token: string;
}

interface AuthState {
  token: string | null;
  email: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'csvStats.jwt';
  private readonly emailKey = 'csvStats.email';
  private readonly state = signal<AuthState>({
    token: this.getTokenFromStorage(),
    email: this.getEmailFromStorage()
  });

  readonly currentEmail: Signal<string | null> = computed(() => this.state().email);
  readonly authenticated: Signal<boolean> = computed(() => !!this.state().token);

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/auth/login`, { email, password })
      .pipe(tap(response => this.persistSession(response)));
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.emailKey);
    this.state.set({ token: null, email: null });
  }

  getToken(): string | null {
    return this.state().token;
  }

  isAuthenticated(): boolean {
    return this.authenticated();
  }

  /** Optional helper kept for quick smoke tests against the backend */
  getMe() {
    return this.http.get(`${API_BASE_URL}/me`, { responseType: 'text' });
  }

  private persistSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.emailKey, response.email);
    this.state.set({ token: response.token, email: response.email });
  }

  private getTokenFromStorage(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  private getEmailFromStorage(): string | null {
    return localStorage.getItem(this.emailKey);
  }
}
