import { Routes } from '@angular/router';
import { authChildGuard, authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    loadComponent: () =>
      import('./features/dashboard/components/dashboard/dashboard.component').then(
        m => m.DashboardComponent
      ),
    children: [
      {
        path: 'uploads',
        loadComponent: () =>
          import('./features/uploads/components/upload-page/upload-page.component').then(
            m => m.UploadPageComponent
          )
      },
      { path: '', pathMatch: 'full', redirectTo: 'uploads' }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
