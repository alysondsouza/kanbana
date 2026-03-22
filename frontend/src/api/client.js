import axios from 'axios'

// Base axios instance — all API calls go through this.
// baseURL is empty: Vite proxy forwards /api/* to Spring Boot (localhost:8080).
const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' }
})

// Attaches the JWT to every request automatically.
// token is passed in from the caller (AuthContext) — client.js has no state.
export function setAuthToken(token) {
  if (token) {
    client.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete client.defaults.headers.common['Authorization']
  }
}

export default client
