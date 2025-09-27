# UI Registration Functionality (Frontend)

## **User Story : Registration Page Navigation**

* **As a** new user
* **I want** to easily find and navigate to the registration page
* **So that** I can start the account creation process

**Story Points:** 2

✅ **Acceptance Criteria:**

* [ ] **AC.1:** A “Sign Up” link or button is visible on the login page
* [ ] **AC.2:** Clicking the link redirects the user to `/register`
* [ ] **AC.3:** The registration page displays a form with required fields (`Name`, `Email`, `Password`, `Confirm Password`)
* [ ] **AC.4:** The registration page follows consistent UI design with the rest of the application

---

## **User Story : Input Validation**

* **As a** new user
* **I want** the registration form to validate my inputs before submission
* **So that** I can fix any mistakes without sending invalid data

**Story Points:** 5

✅ **Acceptance Criteria:**

* [ ] **AC.1:** Email input enforces proper email format and displays inline error messages
* [ ] **AC.2:** Password input enforces minimum 12 characters and shows validation feedback
* [ ] **AC.3:** Confirm Password must match Password field
* [ ] **AC.4:** All validation errors are displayed inline and clearly associated with the field
* [ ] **AC.5:** The “Register” button is disabled if validation fails

---

## **User Story : Submitting Registration Form**

* **As a** new user
* **I want** to submit my registration details
* **So that** my account can be created

**Story Points:** 5

✅ **Acceptance Criteria:**

* [ ] **AC.1:** Clicking “Register” sends a `POST /auth/register` request with JSON body `{name, email, password, confirm_password}`
* [ ] **AC.2:** A loading indicator is displayed while the request is processing
* [ ] **AC.3:** Successful registration displays confirmation message: *“User registered successfully”*
* [ ] **AC.4:** On success, user is redirected either to `/login` (with email pre-filled) or `/dashboard` with token stored securely
* [ ] **AC.5:** If the API returns an error (e.g., duplicate email), the frontend shows a user-friendly error message
