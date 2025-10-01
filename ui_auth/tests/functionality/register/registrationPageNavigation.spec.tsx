// // ./RegistrationPageNavigation.spec.tsx
// /*
//  *  [USER-STORY] Registration Page Navigation
//  *      **As a** new user
//  *      **I want** to easily find and navigate to the registration page
//  *      **So that** I can start the account creation process
//  *
//  *      ✅ **Acceptance Criteria with Equivalence Partitions:**
//  *
//  *          * **AC.1:** A “Sign Up” link or button is visible on the login page
//  *              - VP.1: "Sign Up" link is visible
//  *              - IP.1: "Sign Up" link is missing
//  *
//  *          * **AC.2:** Clicking the link redirects the user to `auth/register`
//  *              - VP.1: Clicking "Sign Up" navigates to `/auth/register`
//  *              - IP.1: Clicking "Sign Up" does not change route
//  *
//  *          * **AC.3:** Registration page displays required fields
//  *              - VP.1: All required fields (`Name`, `Email`, `Password`, `Confirm Password`) are visible
//  *              - IP.1: One or more required fields are missing
//  *
//  *          * **AC.4:** Registration page follows consistent UI design
//  *              - VP.1: Registration page has consistent header and styling
//  *              - IP.1: Registration page header or theme is inconsistent
//  */
// import { render, screen, fireEvent, waitFor } from "@testing-library/react";
// import { describe, it, expect } from "vitest";

// import RegisterForm  from "../../../src/components/register/RegisterForm";

// import userEvent from '@testing-library/user-event';

// // Example components (replace with your actual ones)
// import LoginPage from '../pages/LoginPage';
// import RegisterPage from '../pages/RegisterPage';

// describe('Registration Page Navigation', () => {
//   // --- AC.1 ---
//   test('AC.1 - VP.1: "Sign Up" link is visible on login page', () => {
//     render(
//       <MemoryRouter>
//         <LoginPage />
//       </MemoryRouter>
//     );
//     expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
//   }); 

//   test('AC.1 - IP.1: "Sign Up" link is missing', () => {
//     render(
//       <MemoryRouter>
//         <LoginPage />
//       </MemoryRouter>
//     );
//     expect(screen.queryByRole('link', { name: /nonexistent/i })).not.toBeInTheDocument();
//   });

//   // --- AC.2 ---
//   test('AC.2 - VP.1: Clicking "Sign Up" navigates to /auth/register', async () => {
//     render(
//       <MemoryRouter initialEntries={['/auth/login']}>
//         <Routes>
//           <Route path="/auth/login" element={<LoginPage />} />
//           <Route path="/auth/register" element={<RegisterPage />} />
//         </Routes>
//       </MemoryRouter>
//     );

//     await userEvent.click(screen.getByRole('link', { name: /sign up/i }));
//     expect(await screen.findByRole('heading', { name: /register page/i })).toBeInTheDocument();
//   });

//   test('AC.2 - IP.1: Clicking "Sign Up" does not change route (broken link)', async () => {
//     render(
//       <MemoryRouter initialEntries={['/auth/login']}>
//         <Routes>
//           <Route path="/auth/login" element={<LoginPage />} />
//         </Routes>
//       </MemoryRouter>
//     );

//     await userEvent.click(screen.getByRole('link', { name: /sign up/i }));
//     // Still on login page
//     expect(screen.getByRole('heading', { name: /login page/i })).toBeInTheDocument();
//   });

//   // --- AC.3 ---
//   test('AC.3 - VP.1: Registration form has required fields', () => {
//     render(
//       <MemoryRouter>
//         <RegisterPage />
//       </MemoryRouter>
//     );

//     expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
//     expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
//     expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
//     expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
//   });

//   test('AC.3 - IP.1: Missing required field(s)', () => {
//     render(
//       <MemoryRouter>
//         <RegisterPage />
//       </MemoryRouter>
//     );
//     expect(screen.queryByLabelText(/username/i)).not.toBeInTheDocument();
//   });

//   // --- AC.4 ---
//   test('AC.4 - VP.1: Registration page has consistent UI design', () => {
//     render(
//       <MemoryRouter>
//         <RegisterPage />
//       </MemoryRouter>
//     );

//     expect(screen.getByRole('heading', { name: /register page/i })).toBeInTheDocument();
//     expect(screen.getByTestId('app-container')).toBeInTheDocument(); // assuming shared layout wrapper
//   });

//   test('AC.4 - IP.1: Registration page header inconsistent', () => {
//     render(
//       <MemoryRouter>
//         <RegisterPage />
//       </MemoryRouter>
//     );

//     expect(screen.queryByRole('heading', { name: /login page/i })).not.toBeInTheDocument();
//   });
// });
