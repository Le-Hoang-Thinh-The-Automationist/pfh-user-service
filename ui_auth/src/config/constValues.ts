// API related
export const API_BASE_URL: string =
  import.meta.env.PUBLIC_API_URL || "http://localhost:5000";

export const API_PATH: string = "/api/v1" as const;

// Password related
export const MINIMUM_PASSWORD_LENGTH: number = 12 as const;
export const PASSWORD_FORMAT_REGEX: RegExp =
  /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?":{}|<>]).+$/;

// Email related
// In order to make sense the regex below, please check the test case's data input for email
// ref: ./tests/functionality/register/InputValidation.test.tsx
export const EMAIL_FORMAT_REGEX: RegExp = /^[^\s@]+@[^\s@\.]+(\.[^\s@\.]+)+$/;
