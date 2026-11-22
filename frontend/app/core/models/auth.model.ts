export type UserRole = 'CONTRACT_ADMIN' | 'CALL_CENTER_AGENT';

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  fullName: string;
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
