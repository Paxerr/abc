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

export interface RegisterCredentials {
  email: string;
  password: string;
}

export interface VerifyOtpPayload {
  code: string;
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
   * Register a new user account (CUSTOMER role)
   */
  async register(credentials: RegisterCredentials): Promise<AuthenticationResponse> {
    console.log('[authService] Initiating registration for:', credentials.email);
    const response = await apiClient.post<AuthenticationResponse>('/auth/register', credentials);
    return response.data;
  },

  /**
   * Send or resend OTP verification code to authenticated user's email
   */
  async sendVerificationOtp(): Promise<void> {
    console.log('[authService] Requesting verification OTP (/auth/otp/send)...');
    await apiClient.post('/auth/otp/send');
  },

  /**
   * Verify 6-digit OTP code to activate email
   */
  async verifyOtp(payload: VerifyOtpPayload): Promise<void> {
    console.log('[authService] Verifying OTP code:', payload.code);
    await apiClient.post('/auth/otp/verify', payload);
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

