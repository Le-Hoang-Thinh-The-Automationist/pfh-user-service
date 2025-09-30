// src/mocks/handlers.ts
import { http } from "msw";
import type { RegisterRequestDto, RegisterResponseDto } from "../../../../src/dto/RegisterDto";

export const handlers = [
  http.post("/api/register", async ({ request }) => {
    const body = (await request.json()) as RegisterRequestDto;

    if (body.password !== body.confirmPassword) {
      return new Response(
        JSON.stringify({ error: "Passwords do not match" }),
        { status: 400 }
      );
    }

    const response: RegisterResponseDto = {
      userId: "mock-user-123",
    };

    return new Response(JSON.stringify(response), { status: 200 });
  }),
];
