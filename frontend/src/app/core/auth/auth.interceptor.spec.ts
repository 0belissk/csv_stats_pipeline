import { HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let getTokenSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    getTokenSpy = vi.fn();

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { getToken: getTokenSpy } }]
    });
  });

  it('adds Authorization header when a token is available', async () => {
    getTokenSpy.mockReturnValue('jwt-token');
    const request = new HttpRequest('GET', '/test');

    let forwardedRequest!: HttpRequest<unknown>;

    await TestBed.runInInjectionContext(() =>
      authInterceptor(request, req => {
        forwardedRequest = req;
        return of(new HttpResponse({ status: 200 }));
      })
    );

    expect(forwardedRequest.headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('leaves request untouched when token is missing', async () => {
    getTokenSpy.mockReturnValue(null);
    const request = new HttpRequest('GET', '/test');

    let forwardedRequest!: HttpRequest<unknown>;

    await TestBed.runInInjectionContext(() =>
      authInterceptor(request, req => {
        forwardedRequest = req;
        return of(new HttpResponse({ status: 200 }));
      })
    );

    expect(forwardedRequest.headers.has('Authorization')).toBe(false);
  });
});
