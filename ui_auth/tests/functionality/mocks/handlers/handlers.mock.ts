// src/mocks/handlers.ts
import { delay, http, HttpResponse } from "msw";
import { API_BASE_URL, API_PATH } from "../../../../src/config/constValues";
import {
  TEST_API__CORRECT_EMAIL,
  TEST_API__VALID_PASSWORD,
} from "./test_data/register.data.const";

export const FULL_API_PATH_TEST: string = `${API_BASE_URL}${API_PATH}`;

export const handlers = [
  http.post(`${FULL_API_PATH_TEST}/auth/register`, async ({ request }) => {
    const body = await request.json();

    // Assertions (or checks)
    expect(body).toEqual({
      email: `${TEST_API__CORRECT_EMAIL}`,
      password: `${TEST_API__VALID_PASSWORD}`,
      confirmPassword: `${TEST_API__VALID_PASSWORD}`,
    });

    // Simulate a 2-second delay
    await delay(100);

    return HttpResponse.json(
      { message: "User registered successfully" },
      { status: 201 }
    );
  }),
];
