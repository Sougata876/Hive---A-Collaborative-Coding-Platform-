import { useCallback, useEffect, useRef, useState } from 'react'
import { parseFrame, roomDestinations, roomTopics } from '../lib/stomp'

/**
 * Tracks who is present in a room.
 *
 * Joining is announced explicitly over `/app/room/{id}/presence` because the server cannot infer
 * which room a socket cares about. Every STOMP session has its own id, so multiple tabs of the same
 * user still collapse into a single presence row on the backend.
 */
export function usePresence({ client, roomId }) {
  const [users, setUsers] = useState([])
  const clientRef = useRef(null)

  useEffect(() => {
    clientRef.current = client
  }, [client])

  useEffect(() => {
    if (!client || !roomId) return undefined

    const subscription = client.subscribe(roomTopics.presence(roomId), (frame) => {
      const snapshot = parseFrame(frame.body)
      if (snapshot && Array.isArray(snapshot.users)) setUsers(snapshot.users)
    })

    client.publish({
      destination: roomDestinations.presence(roomId),
      body: JSON.stringify({ action: 'join' }),
    })

    return () => {
      // Only unsubscribe here: the server also clears this session on socket disconnect.
      subscription.unsubscribe()
      setUsers([])
    }
  }, [client, roomId])

  const announceLeave = useCallback(() => {
    if (!clientRef.current?.connected || !roomId) return
    clientRef.current.publish({
      destination: roomDestinations.presence(roomId),
      body: JSON.stringify({ action: 'leave' }),
    })
  }, [roomId])

  return { users, announceLeave }
}
