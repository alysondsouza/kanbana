import { useState } from 'react'
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { useDroppable } from '@dnd-kit/core'
import CardItem from './CardItem'
import client from '../api/client'

// Column — droppable container + sortable card list.
// useDroppable makes this column a valid drop target.
// SortableContext tells dnd-kit which cards belong to this column.
export default function Column({ column, cards, onCardAdded }) {
  const [newTitle, setNewTitle] = useState('')
  const [loading, setLoading] = useState(false)

  // Makes this column a drop target — id must match what onDragEnd checks
  const { setNodeRef } = useDroppable({ id: column.id })

  const handleAddCard = async (e) => {
    e.preventDefault()
    if (!newTitle.trim()) return
    setLoading(true)

    try {
      const res = await client.post(`/api/v1/columns/${column.id}/cards`, {
        title: newTitle
      })
      onCardAdded(column.id, res.data)
      setNewTitle('')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bg-gray-200 rounded p-3 w-64 shrink-0">

      {/* Column title */}
      <h3 className="font-semibold text-sm mb-3">{column.title}</h3>

      {/* SortableContext — card IDs define the sortable order */}
      <SortableContext
        items={cards.map(c => c.id)}
        strategy={verticalListSortingStrategy}
      >
        {/* Droppable area — ref registered so dnd-kit knows this is a drop zone */}
        <div ref={setNodeRef} className="min-h-8">
          {cards.map(card => (
            <CardItem key={card.id} card={card} />
          ))}
        </div>
      </SortableContext>

      {/* Add card form */}
      <form onSubmit={handleAddCard} className="mt-2">
        <input
          value={newTitle}
          onChange={e => setNewTitle(e.target.value)}
          placeholder="Add a card..."
          className="w-full text-sm border px-2 py-1 rounded mb-1"
        />
        <button
          type="submit"
          disabled={loading}
          className="text-sm text-blue-600 hover:underline disabled:opacity-50"
        >
          + Add card
        </button>
      </form>

    </div>
  )
}
