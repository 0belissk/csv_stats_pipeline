import { Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthResponse, AuthService } from '../../../../core/auth/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let loginSpy: ReturnType<typeof vi.fn>;
  let isAuthenticatedSpy: ReturnType<typeof vi.fn>;
  let navigateSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    loginSpy = vi.fn();
    isAuthenticatedSpy = vi.fn().mockReturnValue(false);
    navigateSpy = vi.fn().mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: { login: loginSpy, isAuthenticated: isAuthenticatedSpy } },
        { provide: Router, useValue: { navigate: navigateSpy } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submits credentials and goes to dashboard on success', () => {
    const payload: AuthResponse = { userId: 1, email: 'demo@example.com', token: 'jwt' };
    loginSpy.mockReturnValue(of(payload));

    component.email = 'demo@example.com';
    component.password = 'secret';
    component.onSubmit();

    expect(loginSpy).toHaveBeenCalledWith('demo@example.com', 'secret');
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard', 'uploads']);
  });

  it('shows validation error when email or password missing', () => {
    component.email = '';
    component.password = '';
    component.onSubmit();

    expect(component.errorMessage).toContain('required');
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('surface backend error response when login fails', () => {
    loginSpy.mockReturnValue(
      throwError(() => ({ error: { error: 'Invalid credentials' } }))
    );

    component.email = 'demo@example.com';
    component.password = 'bad';
    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid credentials');
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
