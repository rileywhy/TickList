import { useState, useEffect, type ReactElement } from "react";
import { Routes, Route, Link, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import TickPage from "./pages/TickPage";
import UploadPage from "./pages/UploadPage";
import AccountMenu from "./pages/AccountMenu";
import "./App.css";
import type { CurrentUser } from "./types";

function App() {
  const [currentUser, setCurrentUser] = useState<
    CurrentUser | null | "loading"
  >(() => (localStorage.getItem("user_token") ? "loading" : null));

  useEffect(() => {
    const token = localStorage.getItem("user_token");
    if (token) {
      // Fetch user data using the token
      fetch("/current_user", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
        .then(async (response) => {
          if (!response.ok) {
            // The server saw the token and rejected it -- the session is dead.
            localStorage.removeItem("user_token");
            setCurrentUser(null);
            return;
          }
          const userData = await response.json();
          setCurrentUser({ ...userData, token });
        })
        .catch((error) => {
          // Transport failure (network down, or navigation aborting the request):
          // the token was never judged, so keep it for the next load.
          console.error(error);
          setCurrentUser(null);
        });
    }
  }, []);
  function handleLogin(user: CurrentUser) {
    setCurrentUser(user);
    localStorage.setItem("user_token", user.token);
  }

  function handleLogout() {
    setCurrentUser(null);
    localStorage.removeItem("user_token");
  }

  function requireAuth(render: (user: CurrentUser) => ReactElement) {
    if (currentUser === "loading") return <p>Loading...</p>;
    if (currentUser === null) return <Navigate to="/login" />;
    return render(currentUser);
  }
  const user =
    currentUser !== null && currentUser !== "loading" ? currentUser : null;
  return (
    <>
      <nav className="navbar">
        <Link to="/">Home</Link>
        <Link to="/login">Login</Link>
        <Link to="/register">Register</Link>
        <Link to="/ticks">TickList</Link>
        <Link to="/upload">Import</Link>
      </nav>

      {user && (
        <AccountMenu
          name={user.firstName + " " + user.lastName}
          onLogout={handleLogout}
        />
      )}

      <Routes>
        <Route path="/" element={<h1>Welcome</h1>} />

        <Route
          path="/login"
          element={<LoginPage onLogin={handleLogin} />}
        />

        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/ticks"
          element={requireAuth((user) => (
            <TickPage token={user.token} onAuthExpired={handleLogout} />
          ))}
        />
        <Route
          path="/upload"
          element={requireAuth((user) => (
            <UploadPage token={user.token} onAuthExpired={handleLogout} />
          ))}
        />
      </Routes>
    </>
  );
}

export default App;
