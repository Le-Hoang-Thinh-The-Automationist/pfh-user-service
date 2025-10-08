import React from "react";

export type InputEvent =
  | React.ChangeEvent<HTMLInputElement>
  | React.ClipboardEvent<HTMLInputElement>;

/**
 * Extracts the string value from either a ChangeEvent or ClipboardEvent.
 */
export const getInputValue = (e: InputEvent): string => {
  if ("clipboardData" in e) {
    // ClipboardEvent
    return e.clipboardData.getData("text");
  }
  // ChangeEvent
  return e.target.value;
};

/**
 * Validates an email string and returns an error message if invalid.
 *
 * @param value - The email string to validate.
 * @returns A string containing the error message if invalid, or an empty string if valid.
 */
export const invalidEmailGiveErrorMessage = (value: string): string => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  let message: string = "";

  if (value.trim() === "") {
    message = "Email is required";
  } else if (!regex.test(value)) {
    message = "Invalid email format";
  }

  return message;
};

/**
 * Validates a password string against common security rules.
 *
 * Rules:
 * - Required (not empty)
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one number
 * - At least one special character
 *
 * @param value - The password string to validate.
 * @returns A string containing the error message if invalid, or an empty string if valid.
 */
export const invalidPasswordGiveErrorMessage = (value: string): string => {
  let message: string = "";

  if (value.trim() === "") {
    message = "Password is required";
  } else if (value.length < 8) {
    message = "Password must be at least 8 characters";
  } else if (!/[A-Z]/.test(value)) {
    message = "Password must contain at least one uppercase letter";
  } else if (!/[0-9]/.test(value)) {
    message = "Password must contain at least one number";
  } else if (!/[!@#$%^&*(),.?\":{}|<>]/.test(value)) {
    message = "Password must contain at least one special character";
  }

  return message;
};

/**
 * Validates a confirm password string by checking if it matches the original password.
 *
 * @param value - The confirm password string to validate.
 * @param password - The original password string to compare against.
 * @returns A string containing the error message if invalid, or an empty string if valid.
 */
export const invalidConfirmPasswordGiveErrorMessage = (
  value: string,
  password: string
): string => {
  let message: string = "";

  if (value.trim() === "") {
    message = "Please confirm your password";
  } else if (value !== password) {
    message = "Passwords do not match";
  }

  return message;
};
