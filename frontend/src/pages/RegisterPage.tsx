import { useState } from "react";
import { Link } from "react-router-dom";

function RegisterPage() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleRegister(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    setFieldErrors({});

    let response: Response;
    try {
      response = await fetch("/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          firstName,
          lastName,
          email,
          password,
        }),
      });
    } catch {
      // Transport failure: the request never reached a verdict.
      setMessage("Network error -- check your connection and try again.");
      return;
    }

    if (response.status === 400) {
      // Validation failure: the body is a field -> message map from the backend.
      const errors = await response.json().catch(() => ({}));
      setFieldErrors(errors);
      setMessage("Please fix the highlighted fields.");
      return;
    }

    if (response.status === 409) {
      setFieldErrors({ email: "An account with this email already exists." });
      return;
    }

    if (!response.ok) {
      setMessage(`Could not create account (${response.status}).`);
      return;
    }

    setMessage("Account created successfully!");
    setFirstName("");
    setLastName("");
    setEmail("");
    setPassword("");
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={handleRegister}>
        <h1>Create Account</h1>

        <input
          type="text"
          placeholder="First Name"
          value={firstName}
          onChange={(event) => setFirstName(event.target.value)}
          required
        />
        {fieldErrors.firstName && <p className="field-error">{fieldErrors.firstName}</p>}

        <input
          type="text"
          placeholder="Last Name"
          value={lastName}
          onChange={(event) => setLastName(event.target.value)}
          required
        />
        {fieldErrors.lastName && <p className="field-error">{fieldErrors.lastName}</p>}

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <button type="submit">Register</button>

        {message && <p>{message}</p>}

        <p>
          Already have an account?{" "}
          <Link to="/login">Log In</Link>
        </p>
      </form>
    </main>
  );
}

export default RegisterPage;
