import axios from 'axios'

// Base axios instance — all API calls go through this.
//
// baseURL logic:
//   Dev:  VITE_API_URL is not set → baseURL is '' → Vite proxy forwards /api/* to localhost:8080
//   Prod: VITE_API_URL is set in Cloudflare dashboard → baseURL points directly to app-server VM
//
// This follows the same 12-Factor App pattern as the backend:
//   Backend:  Ansible vault → Docker env var → Spring Boot reads at runtime
//   Frontend: Cloudflare UI → env var → Vite bakes into JS bundle at build time
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: { 'Content-Type': 'application/json' }
})

// Attaches the JWT to every request automatically.
// Called after login/register with the token, called with null on logout.
export function setAuthToken(token) {
  if (token) {
    client.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete client.defaults.headers.common['Authorization']
  }
}

export default client
