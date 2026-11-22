import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.model';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedRole = route.data?.['role'] as UserRole;
  const actualRole = authService.getRole();

  // Route access is based on the role returned in the stored authentication response.
  if (actualRole === expectedRole) {
    return true;
  }
  if (actualRole === 'CONTRACT_ADMIN') {
    return router.createUrlTree(['/admin/dashboard']);
  }
  if (actualRole === 'CALL_CENTER_AGENT') {
    return router.createUrlTree(['/agent/dashboard']);
  }
  return router.createUrlTree(['/auth']);
};
