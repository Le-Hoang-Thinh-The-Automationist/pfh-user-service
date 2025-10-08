import { type FC } from "react";

export const SignUpLink: FC = () => {
  return (
    <a
      href="/auth/register"
      className="signup-link"
      style={{
        display: "inline-block",
        padding: "0.75rem 1.25rem",
        backgroundColor: "#2563eb",
        color: "white",
        borderRadius: "6px",
        textDecoration: "none",
        fontWeight: "600",
      }}
    >
      Sign Up
    </a>
  );
};
