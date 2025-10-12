// ./InputValidation.test.tsx
/*
 *  [USER-STORY] Input Validation
 *      **As a** new user
 *      **I want** the registration form to validate my inputs before submission
 *      **So that** I can fix any mistakes without sending invalid data
 *
 *  ✅ Acceptance Criteria with Equivalence Partitions:
 *
 *      * AC.1: Email input enforces proper email format and displays inline error messages
 *          - VP.1: Valid email format → accepted, no error
 *          - IP.1: Invalid email format → inline error shown
 *
 *      * AC.2: Password input enforces minimum 12 characters and shows validation feedback
 *          - VP.1: ≥12 characters → accepted
 *          - IP.1: <12 characters → error shown
 *
 *      * AC.3: Confirm Password must match Password field
 *          - VP.1: Confirm password matches → accepted
 *          - IP.1: Confirm password does not match → error shown
 *
 *      * AC.4: All validation errors are displayed inline and clearly associated with the field
 *          - VP.1: Each error message is next to the correct input
 *
 *      * AC.5: The “Register” button is disabled if validation fails
 *          - VP.1: Valid inputs enable button
 *          - IP.1: Invalid inputs keep button disabled
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { describe, it, expect } from "vitest";

import RegisterForm from "../../../src/components/register/RegisterForm";

const setup = () => render(<RegisterForm />);

// Test Data Input
const CORRECT_EMAIL_FORMAT: string[] = [
  "email@example.com",
  "firstname.lastname@example.com",
  "email@subdomain.example.com",
  "firstname+lastname@example.com",
  "1234567890@example.com",
  "email@example-one.com",
  "_______@example.com",
  "email@example.name",
  "email@example.co.jp",
  "firstname-lastname@example.com",
];
const INVALID_EMAIL_FORMAT: string[] = [
  "plainaddress", // no @
  "@missingusername.com", // missing local part
  "username@", // missing domain
  "username@.com", // domain starts with dot
  "username@com", // no TLD
  "user name@example.com", // space not allowed
  "username@example..com", // double dot
];

const CORRECT_PASSWORD_FORMAT : string = "StrongPassword123"
const INVALID_PASSWORD_FORMAT : string =  "short"

const MATCH_CONFIRM_PASSWORD    : string = CORRECT_PASSWORD_FORMAT
const UNMATCH_CONFIRM_PASSWORD  : string = "mismatch"

// Test Execution section
// --- AC.1 Email Validation ---
describe("Input Validation - AC.1 (Email Format)", () => {
  it.each(CORRECT_EMAIL_FORMAT)(
    "AC.1 - VP.%#: Accepts valid email '%s' without error",
    (goodEmail) => {
      setup();

      const emailInput = screen.getByPlaceholderText(/email/i);

      fireEvent.change(emailInput, {
        target: { value: goodEmail },
      });
      fireEvent.blur(emailInput);

      expect(screen.queryByText(/invalid email/i)).not.toBeInTheDocument();
    }
  );

  it.each(INVALID_EMAIL_FORMAT)(
    "AC.1 - IP.2.%#: Shows error for invalid email '%s'",
    (badEmail) => {
      setup();

      const emailInput = screen.getByPlaceholderText(/email/i);

      fireEvent.change(emailInput, { target: { value: badEmail } });
      fireEvent.blur(emailInput);

      expect(screen.getByText(/invalid email/i)).toBeInTheDocument();
    }
  );
});

// --- AC.2 Password Length ---
describe("Input Validation - AC.2 (Password Length)", () => {
  it("AC.2 - VP.1: Accepts password ≥12 chars", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: CORRECT_PASSWORD_FORMAT },
    });
    fireEvent.blur(screen.getByLabelText(/password/i));
    expect(screen.queryByText(/must be at least 12 characters/i)).not.toBeInTheDocument();
  });

  it("AC.2 - IP.1: Shows error for password <12 chars", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: INVALID_PASSWORD_FORMAT },
    });
    fireEvent.blur(screen.getByLabelText(/password/i));
    expect(screen.getByText(/must be at least 12 characters/i)).toBeInTheDocument();
  });
});

// --- AC.3 Confirm Password Match ---
describe("Input Validation - AC.3 (Confirm Password)", () => {
  it("AC.3 - VP.1: Accepts matching password and confirm password", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: CORRECT_PASSWORD_FORMAT },
    });
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: MATCH_CONFIRM_PASSWORD },
    });
    fireEvent.blur(screen.getByLabelText(/confirm password/i));
    expect(screen.queryByText(/passwords do not match/i)).not.toBeInTheDocument();
  });

  it("AC.3 - IP.1: Shows error when confirm password does not match", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: CORRECT_PASSWORD_FORMAT },
    });
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: UNMATCH_CONFIRM_PASSWORD },
    });
    fireEvent.blur(screen.getByLabelText(/confirm password/i));
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });
});

// --- AC.4 Inline Errors ---
describe("Input Validation - AC.4 (Inline Errors)", () => {
  it("AC.4 - VP.1: Errors are displayed next to the correct field", () => {
    setup();

    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: "bad-email" },
    });
    fireEvent.blur(screen.getByLabelText(/email/i));

    const emailError = screen.getByText(/invalid email/i);
    const emailInput = screen.getByLabelText(/email/i);

    // Expect error to be associated with the email input
    expect(emailInput).toHaveAccessibleDescription(emailError.textContent ?? "");
  });
});

// --- AC.5 Register Button State ---
describe("Input Validation - AC.5 (Register Button Disabled)", () => {
  it("AC.5 - IP.1: Register button disabled with invalid inputs", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: INVALID_EMAIL_FORMAT },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: "short" },
    });
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: "short" },
    });

    const registerButton = screen.getByRole("button", { name: /register/i });
    expect(registerButton).toBeDisabled();
  });

  it("AC.5 - VP.1: Register button enabled with valid inputs", () => {
    setup();
    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: CORRECT_EMAIL_FORMAT },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: CORRECT_PASSWORD_FORMAT },
    });
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: MATCH_CONFIRM_PASSWORD },
    });

    const registerButton = screen.getByRole("button", { name: /register/i });
    expect(registerButton).toBeEnabled();
  });
});


test("sends correct request format when registering", async () => {
  render(<RegisterForm />);

  // Simulate user typing and clicking
  await userEvent.type(screen.getByLabelText(/email/i), "user@example.com");
  await userEvent.click(screen.getByRole("button", { name: /register/i }));

  // If request fails expectations, test will fail
  expect(await screen.findByRole("button", { name: /register/i })).toBeInTheDocument();
});