import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { DndContext, closestCenter, DragOverlay } from '@dnd-kit/core'
import Column from '../components/Column'
import CardItem from '../components/CardItem'
import client from '../api/client'

export default function BoardViewPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [columns, setColumns] = useState([])
  const [cards, setCards] = useState({})       // { [columnId]: Card[] }
  const [newColTitle, setNewColTitle] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeCard, setActiveCard] = useState(null)  // card being dragged

  // Fetch columns then cards for each column in parallel
  useEffect(() => {
    const load = async () => {
      try {
        const colRes = await client.get(`/api/v1/boards/${id}/columns`)
        const cols = colRes.data
        setColumns(cols)

        const cardEntries = await Promise.all(
          cols.map(async col => {
            const cardRes = await client.get(`/api/v1/columns/${col.id}/cards`)
            return [col.id, cardRes.data]
          })
        )
        setCards(Object.fromEntries(cardEntries))
      } catch {
        setError('Failed to load board')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  const handleCardAdded = (columnId, newCard) => {
    setCards(prev => ({
      ...prev,
      [columnId]: [...(prev[columnId] || []), newCard]
    }))
  }

  const handleAddColumn = async (e) => {
    e.preventDefault()
    if (!newColTitle.trim()) return
    try {
      const res = await client.post(`/api/v1/boards/${id}/columns`, {
        title: newColTitle
      })
      setColumns(prev => [...prev, res.data])
      setCards(prev => ({ ...prev, [res.data.id]: [] }))
      setNewColTitle('')
    } catch {
      setError('Failed to create column')
    }
  }

  // Store the card being dragged so DragOverlay can render it
  const handleDragStart = ({ active }) => {
    const card = Object.values(cards).flat().find(c => c.id === active.id)
    setActiveCard(card ?? null)
  }

  const handleDragEnd = async ({ active, over }) => {
    setActiveCard(null)              // clear overlay regardless of outcome

    if (!over) return
    if (active.id === over.id) return

    const cardId = active.id

    // over.id may be a card ID (dropped on a card) or column ID (dropped on background)
    const targetColumnId =
      Object.keys(cards).find(colId =>
        cards[colId].some(c => c.id === over.id)
      ) ?? over.id

    const sourceColumnId = Object.keys(cards).find(colId =>
      cards[colId].some(c => c.id === cardId)
    )

    if (!sourceColumnId) return
    if (sourceColumnId === targetColumnId) return

    const card = cards[sourceColumnId].find(c => c.id === cardId)

    // Optimistic update
    setCards(prev => ({
      ...prev,
      [sourceColumnId]: prev[sourceColumnId].filter(c => c.id !== cardId),
      [targetColumnId]: [...(prev[targetColumnId] || []), { ...card, columnId: targetColumnId }]
    }))

    try {
      await client.patch(`/api/v1/cards/${cardId}/move`, { targetColumnId })
    } catch {
      // Revert on failure
      setCards(prev => ({
        ...prev,
        [sourceColumnId]: [...(prev[sourceColumnId] || []), card],
        [targetColumnId]: prev[targetColumnId].filter(c => c.id !== cardId)
      }))
      setError('Failed to move card')
    }
  }

  if (loading) return <p className="p-8">Loading...</p>
  if (error)   return <p className="p-8 text-red-500">{error}</p>

  return (
    <div className="min-h-screen bg-gray-100">

      <nav className="bg-white shadow px-8 py-4 flex items-center gap-4">
        <button
          onClick={() => navigate('/boards')}
          className="text-sm text-blue-600 hover:underline"
        >
          ← Boards
        </button>
      </nav>

      <div className="p-8">
        <DndContext
          collisionDetection={closestCenter}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
        >
          <div className="flex gap-4 items-start overflow-x-auto pb-4">

            {columns.map(col => (
              <Column
                key={col.id}
                column={col}
                cards={cards[col.id] || []}
                onCardAdded={handleCardAdded}
              />
            ))}

            <form
              onSubmit={handleAddColumn}
              className="bg-gray-200 rounded p-3 w-64 shrink-0"
            >
              <input
                value={newColTitle}
                onChange={e => setNewColTitle(e.target.value)}
                placeholder="Add a column..."
                className="w-full text-sm border px-2 py-1 rounded mb-1"
              />
              <button
                type="submit"
                className="text-sm text-blue-600 hover:underline"
              >
                + Add column
              </button>
            </form>

          </div>

          {/* DragOverlay — renders a floating copy of the card that follows the cursor.
              The original card stays in its column (faded via isDragging in CardItem).
              Portal renders it outside the column DOM so it floats above everything. */}
          <DragOverlay>
            {activeCard ? <CardItem card={activeCard} /> : null}
          </DragOverlay>

        </DndContext>
      </div>
    </div>
  )
}
