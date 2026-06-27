import { useState } from "react";
import { Routes, Route, Link, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import TicketPage from "./pages/TicketPage";
import AccountMenu from "./pages/AccountMenu";
import "./App.css";
import type { CurrentUser } from "./types";

function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);


  function handleLogout() {
    setCurrentUser(null);
    
  }

  return (
    <>
      <nav className="navbar">
        <Link to="/">Home</Link>
        <Link to="/login">Login</Link>
        <Link to="/register">Register</Link>
        <Link to="/tickets">Tickets</Link>
      </nav>

      {currentUser!== null && <AccountMenu name={currentUser.firstName+" "+currentUser.lastName} onLogout={handleLogout} />}

      <Routes>
        <Route path="/" element={<h1>Welcome</h1>} />

        <Route
          path="/login"
          element={<LoginPage setCurrentUser={setCurrentUser} />}
        />

        <Route path="/register" element={<RegisterPage  />} />

        <Route
          path="/tickets"
          element={
            currentUser !== null ? (
              <TicketPage
                token={currentUser.token}
                onAuthExpired={handleLogout}
              />
            ) : (
              <Navigate to="/login" />
            )
          }
        />
      </Routes>
    </>
  );
}

export default App;
