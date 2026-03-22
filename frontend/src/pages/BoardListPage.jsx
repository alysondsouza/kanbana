import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { setAuthToken } from '../api/client'
import client from '../api/client'

export default function BoardListPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [boards, setBoards] = useState([])
  const [newTitle, setNewTitle] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  // Fetch all boards on mount
  useEffect(() => {
    client.get('/api/v1/boards')
      .then(res => setBoards(res.data))
      .catch(() => setError('Failed to load boards'))
      .finally(() => setLoading(false))
  }, [])

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!newTitle.trim()) return

    try {
      const res = await client.post('/api/v1/boards', { title: newTitle })
      setBoards([...boards, res.data])  // append new board to list
      setNewTitle('')                    // clear input
    } catch {
      setError('Failed to create board')
    }
  }

  const handleLogout = () => {
    logout()                // clear token from AuthContext
    setAuthToken(null)      // remove Authorization header from axios
    navigate('/login')
  }

  if (loading) return <p className="p-8">Loading...</p>

  return (
    <div className="min-h-screen bg-gray-100">

      {/* Navbar */}
      <nav className="bg-white shadow px-8 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold">Kanbana</h1>
        <button
          onClick={handleLogout}
          className="text-sm text-red-500 hover:underline"
        >
          Logout
        </button>
      </nav>

      <div className="max-w-4xl mx-auto p-8">

        {error && <p className="text-red-500 mb-4">{error}</p>}

        {/* Create board form */}
        <form onSubmit={handleCreate} className="flex gap-2 mb-8">
          <input
            value={newTitle}
            onChange={e => setNewTitle(e.target.value)}
            placeholder="New board title"
            className="flex-1 border px-3 py-2 rounded"
          />
          <button
            type="submit"
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Create
          </button>
        </form>

        {/* Board list */}
        {boards.length === 0 ? (
          <p className="text-gray-500">No boards yet. Create one above.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
            {boards.map(board => (
              <div
                key={board.id}
                onClick={() => navigate(`/boards/${board.id}`)}
                className="bg-white rounded shadow p-4 cursor-pointer hover:shadow-md transition"
              >
                <h2 className="font-semibold">{board.title}</h2>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}
