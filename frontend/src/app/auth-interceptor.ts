import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  console.log('AUTH INTERCEPTOR HIT:', req.url);

  const token = localStorage.getItem('jwt');

  if (token) {
    console.log('ATTACHING TOKEN');
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  } else {
    console.log('NO TOKEN FOUND');
  }

  return next(req);
};
