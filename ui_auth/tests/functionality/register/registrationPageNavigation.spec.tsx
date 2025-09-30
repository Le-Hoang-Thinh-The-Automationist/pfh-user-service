// ./registration-page-navigation.spec.ts
/*
 *  [USER-STORY] Registration Page Navigation
 *      **As a** new user
 *      **I want** to easily find and navigate to the registration page
 *      **So that** I can start the account creation process
 *
 *      ✅ **Acceptance Criteria with Equivalence Partitions:**
 *
 *          * **AC.1:** A “Sign Up” link or button is visible on the login page
 *              - VP.1: "Sign Up" link is present and visible
 *              - IP.1: "Sign Up" link is missing or hidden
 *
 *          * **AC.2:** Clicking the link redirects the user to /auth/register
 *              - VP.1: Clicking navigates to /auth/register
 *              - IP.1: Clicking navigates to wrong URL
 *
 *          * **AC.3:** The registration page displays a form with required fields
 *              - VP.1: All required fields (Name, Email, Password, Confirm Password) are visible
 *              - IP.1: One or more required fields are missing
 *
 *          * **AC.4:** The registration page follows consistent UI design
 *              - VP.1: Page contains expected heading and consistent styling classes
 *              - IP.1: Page heading or design tokens are inconsistent
 */
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect } from "vitest";

import RegisterForm  from "../../../src/components/register/RegisterForm";


test("registers user successfully with matching passwords", async () => {
  render(<RegisterForm />);

  fireEvent.change(screen.getByPlaceholderText("Email"), {
    target: { value: "test@example.com" },
  });
  fireEvent.change(screen.getByPlaceholderText("Password"), {
    target: { value: "secret123" },
  });
  fireEvent.change(screen.getByPlaceholderText("Confirm password"), {
    target: { value: "secret123" },
  });

  fireEvent.click(screen.getByText("Register"));

});