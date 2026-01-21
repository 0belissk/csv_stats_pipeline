import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse, AuthService } from './auth.service';

const LOGIN_URL = 'http://localhost:8080/auth/login';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('logs in and persists the JWT/email pair', () => {
    const mockResponse: AuthResponse = { userId: 42, email: 'demo@example.com', token: 'jwt-token' };
    let payload: AuthResponse | null = null;

    service.login('demo@example.com', 'secret').subscribe(res => (payload = res));

    const request = httpMock.expectOne(LOGIN_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'demo@example.com', password: 'secret' });

    request.flush(mockResponse);

    expect(payload).toEqual(mockResponse);
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getToken()).toBe('jwt-token');
    expect(service.currentEmail()).toBe('demo@example.com');
  });

  it('clears auth state on logout', () => {
    const mockResponse: AuthResponse = { userId: 1, email: 'user@example.com', token: 'abc' };
    service.login('user@example.com', 'pw').subscribe();
    httpMock.expectOne(LOGIN_URL).flush(mockResponse);

    service.logout();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(service.currentEmail()).toBeNull();
  });
});
