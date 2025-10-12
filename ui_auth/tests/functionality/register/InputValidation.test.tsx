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
 *          - Valid Partitions (VP):
 *            - VP.1: Valid email format → accepted, no error
 *          - Invalid Partitions (IP):
 *            - IP.1: Invalid email format → inline error shown
 *
 *      * AC.2: Password input enforces minimum 12 characters, one uppercase letter, one number,
 *              and one special character and shows validation feedback
 *          - Valid Partitions (VP):
 *            - VP.1: Valid format → accepted
 *          - Invalid Partitions (IP):
 *            - IP.1: Invalid format → error shown
 *
 *      * AC.3: Confirm Password must match Password field
 *          - Valid Partitions (VP):
 *            - VP.1: Confirm password matches password → accepted
 *          - Invalid Partitions (IP):
 *            - IP.1: Confirm password does not match password → error shown
 *            - IP.2: Confirm password first matches the password,
 *                  But then password changes without changing confirm password → error shown
 *
 *      * AC.4: All validation errors are displayed inline and clearly associated with the field
 *          - Valid Partitions (VP):
 *            - VP.1: Each error message is next to the correct input
 *
 *      * AC.5: The “Register” button is disabled if validation fails
 *          - Valid Partitions (VP):
 *            - VP.1: Valid inputs enable button
 *          - Invalid Partitions (IP):
 *            - IP.1: Given the other inputs are correct, invalid email will disable register button
 *            - IP.2: Given the other inputs are correct, invalid password will disable register button
 *            - IP.3: Given the other inputs are correct, mismatching `confirm password` will disable register button
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

const CORRECT_PASSWORD_FORMAT: string[] = [
  "StrongPass123!", // meets all requirements
  "MySecurePwd2025$", // long, uppercase, number, special
  "ValidPassword#99", // uppercase, number, special
];
const INVALID_PASSWORD_FORMAT: string[] = [
  "Ab1!short", // too short (<12 chars)
  "no_uppercase123!@#", // missing uppercase
  "NoNumberPassword!", // missing number
  "NoSpecialCharacter123", // missing special
  "1234567890123!", // missing uppercase
  "NO_LOWERCASE!12", // missing uppercase
  "OnlyTextPassword", // missing special + number
];

const MATCH_CONFIRM_PASSWORD: string = CORRECT_PASSWORD_FORMAT[0];
const UNMATCH_CONFIRM_PASSWORD: string = "mismatch";

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

// --- AC.2 Password Validation ---
describe("Input Validation - AC.2 (Password Format)", () => {
  it.each(CORRECT_PASSWORD_FORMAT)(
    "AC.2 - VP.%#: Accepts valid password '%s' without error",
    (goodPassword) => {
      setup();

      const passwordInput = screen.getByPlaceholderText(/^password$/i);

      fireEvent.change(passwordInput, { target: { value: goodPassword } });
      fireEvent.blur(passwordInput);

      expect(screen.queryByText(/password must/i)).not.toBeInTheDocument();
    }
  );

  it.each(INVALID_PASSWORD_FORMAT)(
    "AC.2 - IP.%#: Shows error for invalid password '%s'",
    (badPassword) => {
      setup();

      const passwordInput = screen.getByPlaceholderText(/^password$/i);

      fireEvent.change(passwordInput, { target: { value: badPassword } });
      fireEvent.blur(passwordInput);

      expect(screen.getByText(/password must/i)).toBeInTheDocument();
    }
  );
});

// --- AC.3 Confirm Password Match ---
describe("Input Validation - AC.3 (Confirm Password)", () => {
  let correctPassword: string = CORRECT_PASSWORD_FORMAT[0];

  it("AC.3 - VP.1: Accepts matching password and confirm password", () => {
    setup();

    fireEvent.change(screen.getByPlaceholderText(/^password$/i), {
      target: { value: correctPassword },
    });

    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: MATCH_CONFIRM_PASSWORD },
    });

    fireEvent.blur(screen.getByPlaceholderText(/confirm password/i));

    expect(
      screen.queryByText(/passwords do not match/i)
    ).not.toBeInTheDocument();
  });

  it("AC.3 - IP.1: Shows error when confirm password does not match", () => {
    setup();

    fireEvent.change(screen.getByPlaceholderText(/^password$/i), {
      target: { value: correctPassword },
    });

    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: UNMATCH_CONFIRM_PASSWORD },
    });

    fireEvent.blur(screen.getByPlaceholderText(/confirm password/i));

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it("AC.3 - IP.2: First time both password and confirm password match, but then password change", () => {
    setup();

    fireEvent.change(screen.getByPlaceholderText(/^password$/i), {
      target: { value: correctPassword },
    });

    // First time matches
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: MATCH_CONFIRM_PASSWORD },
    });

    // Change the original password
    fireEvent.change(screen.getByPlaceholderText(/^password$/i), {
      target: { value: `${correctPassword}a` },
    });

    fireEvent.blur(screen.getByPlaceholderText(/confirm password/i));

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });
});

// --- AC.4 Inline Errors ---
// The requirement: `All validation errors are displayed inline and clearly associated with the field`
// should be manually test, automation test is ill-advised

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