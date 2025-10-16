// ./SubmitRegistration.test.tsx
/*
 *  [USER-STORY] Submitting Registration Form
 *      **As a** new user
 *      **I want** to submit my registration details
 *      **So that** my account can be created
 *
 *  ✅ Acceptance Criteria:
 *
 *      * AC.1: Clicking “Register” sends a POST api/v1/auth/register request
 *              with JSON body { email, password, confirm_password }
 *
 *      * AC.2: A loading indicator is displayed while the request is processing
 *
 *      * AC.3: Successful registration displays confirmation message:
 *              "User registered successfully"
 *
 *      * AC.4: On success, user is redirected to /auth/login with email pre-filled
 *
 *      * AC.5: If the API returns an error (e.g., duplicate email),
 *              a user-friendly error message is displayed
 */
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import {
  TEST_API__CORRECT_EMAIL,
  TEST_API__VALID_PASSWORD,
} from "../mocks/handlers/test_data/register.data.const";
import RegisterForm from "../../../src/components/register/RegisterForm";

// Mock navigation and fetch
const mockNavigate = vi.fn();
const mockFetch = vi.fn();

vi.mock("react-router-dom", () => ({
  ...vi.importActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

global.fetch = mockFetch as any;

/* -------------------------------------------------------------------------- */
/*                                Test Setup                                  */
/* -------------------------------------------------------------------------- */

const setup = () => render(<RegisterForm />);

/* -------------------------------------------------------------------------- */
/*                         Acceptance Criteria Testing                        */
/* -------------------------------------------------------------------------- */

/**
 *  --- AC.1 ---
 *  Clicking “Register” sends a POST request to api/v1/auth/register with correct payload
 */
describe("Submit Registration - AC.1 (POST request on register)", () => {
  it("AC.1 - VP.1: Sends correct POST payload when user clicks register", async () => {
    setup(); // renders the component

    const user = userEvent.setup();

    await user.type(
      screen.getByPlaceholderText(/email/i),
      TEST_API__CORRECT_EMAIL
    );
    await user.type(
      screen.getByPlaceholderText(/^password$/i),
      TEST_API__VALID_PASSWORD
    );
    await user.type(
      screen.getByPlaceholderText(/confirm password/i),
      TEST_API__VALID_PASSWORD
    );

    // After the button is clicked, then the API test shall be check be mock servers
    await user.click(screen.getByRole("button", { name: /register/i }));
  });
});

// /**
//  *  --- AC.2 ---
//  *  Displays loading indicator while request is processing
//  */
// describe("Submit Registration - AC.2 (Loading Indicator)", () => {
//   it("AC.2 - VP.1: Shows loading indicator during API call", async () => {
//     setup();
//     const user = userEvent.setup();

//     await user.type(
//       screen.getByPlaceholderText(/email/i),
//       TEST_API__CORRECT_EMAIL
//     );
//     await user.type(
//       screen.getByPlaceholderText(/^password$/i),
//       TEST_API__VALID_PASSWORD
//     );
//     await user.type(
//       screen.getByPlaceholderText(/confirm password/i),
//       TEST_API__VALID_PASSWORD
//     );

//     await user.click(screen.getByRole("button", { name: /register/i }));

//     // Expect loading indicator visible
//     expect(screen.getByText(/loading/i)).toBeInTheDocument();

//     /*
//      *  Cover for AC3 - VP.1 of [Submit Registration]
//      */
//     const successMessage = await screen.findByText(
//       /user registered successfully/i
//     );
//     expect(successMessage).toBeInTheDocument();
//   });
// });

// /**
//  *  --- AC.3 ---
//  *  Successful registration displays confirmation message
//  */
// describe("Submit Registration - AC.3 (Success Message)", () => {
//   it("AC.3 - VP.1: Shows success message upon successful registration", async () => {
//     // The test case expectation is already covered by AC2 - VP.1 of [Submit Registration]
//   });
// });

// /**
//  *  --- AC.4 ---
//  *  Redirects user to login page with email pre-filled on success
//  */
// describe("Submit Registration - AC.4 (Redirect on Success)", () => {
//   it("AC.4 - VP.1: Navigates to /auth/login with email in state after success", async () => {
//     setup();
//     const user = userEvent.setup();

//     mockFetch.mockResolvedValueOnce({
//       ok: true,
//       json: async () => ({ message: "User registered successfully" }),
//     });

//     await user.type(screen.getByPlaceholderText(/email/i), TEST_EMAIL);
//     await user.type(screen.getByPlaceholderText(/password/i), TEST_PASSWORD);
//     await user.type(
//       screen.getByPlaceholderText(/confirm password/i),
//       TEST_PASSWORD
//     );

//     await user.click(screen.getByRole("button", { name: /register/i }));

//     await waitFor(() => {
//       expect(mockNavigate).toHaveBeenCalledWith("/auth/login", {
//         state: { email: TEST_EMAIL },
//       });
//     });
//   });
// });

// /**
//  *  --- AC.5 ---
//  *  API error displays user-friendly error message
//  */
// describe("Submit Registration - AC.5 (Error Handling)", () => {
//   it("AC.5 - IP.1: Displays friendly error message for duplicate email", async () => {
//     setup();
//     const user = userEvent.setup();

//     mockFetch.mockResolvedValueOnce({
//       ok: false,
//       json: async () => ({ message: "Email already exists" }),
//     });

//     await user.type(screen.getByPlaceholderText(/email/i), TEST_EMAIL);
//     await user.type(screen.getByPlaceholderText(/password/i), TEST_PASSWORD);
//     await user.type(
//       screen.getByPlaceholderText(/confirm password/i),
//       TEST_PASSWORD
//     );

//     await user.click(screen.getByRole("button", { name: /register/i }));

//     await waitFor(() => {
//       expect(screen.getByText(/email already exists/i)).toBeInTheDocument();
//     });
//   });
// });
