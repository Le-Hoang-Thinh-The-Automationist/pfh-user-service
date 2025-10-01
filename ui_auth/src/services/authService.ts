import { API_PATH } from "../config/constValues.ts";
import type { RegisterRequestDto, RegisterResponseDto } from "../dto/RegisterDto.ts";
import { apiClient } from "./apiClient.ts";

export const registerUser = (data : RegisterRequestDto) =>
  apiClient.post<RegisterResponseDto>(`${API_PATH}/auth/register`, data);

export const loginUser = (data: { email: string; password: string }) =>
  apiClient.post(`${API_PATH}/auth/login`, data);

export const logoutUser = () => {
  // Optionally inform backend, or just clear frontend token
  return apiClient.post(`${API_PATH}/auth/logout`);
};
