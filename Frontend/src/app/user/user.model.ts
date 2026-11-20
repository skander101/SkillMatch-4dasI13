export type Role = 'CANDIDAT' | 'RECRUTEUR' | 'ADMIN';

export interface User {
  id: number;
  nom: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nom: string;
  email: string;
  password: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  user: User;
}
