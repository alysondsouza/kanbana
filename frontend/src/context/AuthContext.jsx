import { createContext, useContext, useState } from 'react'

// AuthContext — holds the JWT and exposes login/logout to the whole app.
// JWT lives in memory only (useState) — never written to localStorage.
// If the user refreshes the page, the token is gone and they log in again.
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null)

  // Called by LoginPage and RegisterPage on success
  const login = (jwt) => setToken(jwt)

  // Called by Navbar logout button
  const logout = () => setToken(null)

  return (
    <AuthContext.Provider value={{ token, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// Custom hook — any component can call useAuth() to get token/login/logout
export function useAuth() {
  return useContext(AuthContext)
}
