import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'

// CardItem — draggable card using dnd-kit useSortable hook.
// useSortable gives us: listeners (drag events), attributes (a11y),
// setNodeRef (DOM ref), and transform/transition for smooth animation.
export default function CardItem({ card }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: card.id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,  // fade card while dragging
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className="bg-white rounded shadow-sm p-3 mb-2 cursor-grab active:cursor-grabbing"
    >
      <p className="text-sm font-medium">{card.title}</p>
      {card.description && (
        <p className="text-xs text-gray-500 mt-1">{card.description}</p>
      )}
    </div>
  )
}
