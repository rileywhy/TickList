import { useState } from "react";
import type { CurrentUser } from "../types";
type LoginPageProps = {
  setCurrentUser: (user: CurrentUser) => void;
};

function LoginPage({ setCurrentUser }: LoginPageProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  async function handleLogin(event: React.FormEvent) {
    event.preventDefault();

    const response = await fetch("/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email,
        password,
      }),
    });

    if (!response.ok) {
      setMessage("Invalid email or password.");
      return;
    }

    const user = await response.json();
    console.log(user);

    setCurrentUser(user);
    setMessage("Login successful!");
  }

  return (
    <div>
      <h1>Login</h1>

      <form onSubmit={handleLogin}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <button type="submit">Log In</button>
      </form>

      <p>{message}</p>
    </div>
  );
}

export default LoginPage;
