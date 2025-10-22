import { useEffect, useState } from "react";
import { registerUser } from "../../services/authService";
import type { RegisterRequestDto } from "../../dto/RegisterDto";
import {
  getInputValue,
  invalidConfirmPasswordGiveErrorMessage,
  invalidEmailGiveErrorMessage,
  invalidPasswordGiveErrorMessage,
  type InputEvent,
} from "../../utils/inputUtils";
import RegisterLoadingIndicator from "./RegisterLoading";

const RegisterForm: React.FC = () => {
  // =============================  REACT HOOKS AND OTHER VARIABLES ======================
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [confirmPasswordError, setConfirmPasswordError] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const isDisabled =
    !!emailError ||
    !!passwordError ||
    !!confirmPasswordError ||
    !email ||
    !password ||
    !confirmPassword;

  // =============================  REACT EVENT HANDLER ======================
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setSuccessMessage("");

    const payload: RegisterRequestDto = { email, password, confirmPassword };

    try {
      const result = await registerUser(payload);

      if (result?.data?.message) {
        setSuccessMessage(result.data.message);
      }
    } catch (error) {
      console.error("Registration failed:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleEmailChange = (e: InputEvent) => {
    const value = getInputValue(e);
    setEmail(value);
    setEmailError(invalidEmailGiveErrorMessage(value));
  };

  const handlePasswordChange = (e: InputEvent) => {
    const value = getInputValue(e);
    setPassword(value);
    setPasswordError(invalidPasswordGiveErrorMessage(value));
  };

  const handleConfirmPasswordChange = (e: InputEvent) => {
    const value = getInputValue(e);
    setConfirmPassword(value);
    setConfirmPasswordError(
      invalidConfirmPasswordGiveErrorMessage(value, password)
    );
  };

  // On event change of password, re-check the confirm password
  useEffect(() => {
    if (confirmPassword) {
      setConfirmPasswordError(
        invalidConfirmPasswordGiveErrorMessage(confirmPassword, password)
      );
    }
  }, [password]);
  // =============================  REACT COMPONENT ======================
  return (
    <form className="auth-register__register-form">
      <div style={{ display: "flex", flexDirection: "column" }}>
        {/* Email Field */}
        <div
          className="auth-register__form-input"
          style={{ display: "flex", flexDirection: "column" }}
        >
          <label htmlFor="email" className="">
            Email<span className="">*</span>
          </label>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={handleEmailChange}
            onPaste={handleEmailChange}
          />
          {emailError && (
            <span className="auth-register__input-error-display">
              {emailError}
            </span>
          )}
        </div>
        {/* Password Field */}
        <div
          className="auth-register__form-input"
          style={{ display: "flex", flexDirection: "column" }}
        >
          <label htmlFor="password" className="">
            Password<span className="">*</span>
          </label>
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={handlePasswordChange}
            onPaste={handlePasswordChange}
          />
          {passwordError && (
            <span className="auth-register__input-error-display">
              {passwordError}
            </span>
          )}
        </div>
        {/* Confirm Password Field */}
        <div
          className="auth-register__form-input"
          style={{ display: "flex", flexDirection: "column" }}
        >
          <label htmlFor="password" className="">
            Confirm Password<span className="">*</span>
          </label>
          <input
            type="password"
            placeholder="Confirm password"
            value={confirmPassword}
            onChange={handleConfirmPasswordChange}
            onPaste={handleConfirmPasswordChange}
          />
          {confirmPasswordError && (
            <span className="auth-register__input-error-display">
              {confirmPasswordError}
            </span>
          )}
        </div>
        <button
          className="auth-register__submit-button"
          type="submit"
          onClick={handleSubmit}
          disabled={isDisabled || isLoading}
        >
          {isLoading ? "Registering..." : "Register"}
        </button>
      </div>
      {isLoading ? (
        <RegisterLoadingIndicator />
      ) : successMessage ? (
        <div className="api-message">{successMessage}</div>
      ) : null}
    </form>
  );
};

export default RegisterForm;
