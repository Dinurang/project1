export type UserRole = 'CONTRACT_ADMIN' | 'CALL_CENTER_AGENT';

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  fullName: string;
  // These fields are required by their corresponding role on the backend.
  employeeCode?: string;
  contactNo?: string;
  agentCode?: string;
  deskExtension?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  fullName: string;
  role: UserRole;
}
