export interface RegisterRequestDto {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterResponseDto {
  userId: string;
  email: string;
  message: string;
}
