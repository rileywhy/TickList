import { useState } from "react";

type LoginPageProps = {
setIsLoggedIn: (value: boolean) => void;
};
function LoginPage({ setIsLoggedIn }: LoginPageProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  


  function handleLogin(event: React.FormEvent) {
    event.preventDefault();

    fetch("http://localhost:8080/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email,
        password,
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("Login failed");
        }
        return response.text();
      })
      .then(() => {
        setMessage("Login successful");
        setIsLoggedIn(true);
      })
      .catch(() => {
        setMessage("Invalid email or password");
      });
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