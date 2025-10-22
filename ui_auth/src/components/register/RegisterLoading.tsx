import React from "react";

const RegisterLoadingIndicator: React.FC = () => {
  return (
    <div className="register-loading-container">
      <div className="spinner" />
      <p className="loading-text">Registering your account, please wait…</p>
    </div>
  );
};

export default RegisterLoadingIndicator;
