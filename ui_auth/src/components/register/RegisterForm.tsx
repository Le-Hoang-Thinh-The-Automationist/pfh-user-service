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

const RegisterForm: React.FC = () => {
  // =============================  REACT HOOKS AND OTHER VARIABLES ======================
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [confirmPasswordError, setConfirmPasswordError] = useState("");

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

    const payload: RegisterRequestDto = { email, password, confirmPassword };

    const result = await registerUser(payload);

    console.log("Register result:", result);
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
        {emailError && (
          <span className="auth-register__input-error-display">
            {emailError}
          </span>
        )}
        <input
          className="auth-register__form-input"
          type="email"
          placeholder="Email"
          value={email}
          onChange={handleEmailChange}
          onPaste={handleEmailChange}
        />
        {passwordError && (
          <span className="auth-register__input-error-display">
            {passwordError}
          </span>
        )}
        <input
          className="auth-register__form-input"
          type="password"
          placeholder="Password"
          value={password}
          onChange={handlePasswordChange}
          onPaste={handlePasswordChange}
        />
        {confirmPasswordError && (
          <span className="auth-register__input-error-display">
            {confirmPasswordError}
          </span>
        )}
        <input
          className="auth-register__form-input"
          type="password"
          placeholder="Confirm password"
          value={confirmPassword}
          onChange={handleConfirmPasswordChange}
          onPaste={handleConfirmPasswordChange}
        />
        <button
          className="auth-register__submit-button"
          type="submit"
          onClick={handleSubmit}
          disabled={isDisabled}
        >
          Register
        </button>
      </div>
    </form>
  );
};

export default RegisterForm;
