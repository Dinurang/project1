import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Logged-in users should not need to remain on the login/signup page.
  if (!authService.isLoggedIn()) {
    return true;
  }
  const role = authService.getRole();
  return router.createUrlTree([role === 'CONTRACT_ADMIN' ? '/admin/dashboard' : '/agent/dashboard']);
};
