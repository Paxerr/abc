import { apiClient } from './apiClient';
import type { Role } from '../types';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthenticationResponse {
  accessToken: string;
  userId: string;
  email: string;
  role: Role;
}

export interface UserProfileResponse {
  id: string;
  email: string;
  fullName: string | null;
  avatarUrl: string | null;
  locale: string | null;
  role: Role;
  isEmailVerified: boolean;
}

export const authService = {
  /**
   * Log in with email and password
   */
  async login(credentials: LoginCredentials): Promise<AuthenticationResponse> {
    console.log('[authService] Initiating login for:', credentials.email);
    const response = await apiClient.post<AuthenticationResponse>('/auth/login', credentials);
    return response.data;
  },

  /**
   * Log out current user and clear server-side session/cookies
   */
  async logout(): Promise<void> {
    console.log('[authService] Logging out...');
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      console.warn('[authService] Server logout notification failed, continuing local cleanup:', error);
    }
  },

  /**
   * Fetch current authenticated user's profile
   */
  async getMyProfile(): Promise<UserProfileResponse> {
    console.log('[authService] Fetching profile (/users/me)...');
    const response = await apiClient.get<UserProfileResponse>('/users/me');
    return response.data;
  },
};
