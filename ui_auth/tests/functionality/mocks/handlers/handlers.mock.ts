// src/mocks/handlers.ts
import { http, HttpResponse } from "msw";
import type { RegisterRequestDto, RegisterResponseDto } from "../../../../src/dto/RegisterDto";
import { API_BASE_URL, API_PATH } from "../../../../src/config/constValues";

const FULL_API_PATH_TEST : string = `${API_BASE_URL}${API_PATH}`

export const handlers = [
  http.post(`${FULL_API_PATH_TEST}/auth/register`, async ({ request }) => {
    const body = await request.json();

    // Assertions (or checks)
    expect(body).toEqual({
      email: "user@example.com",
      password: "",
      confirmPassword: "",
    });

    return HttpResponse.json(
      { message: "User registered successfully" },
      { status: 201 }
    );
  }),
];
