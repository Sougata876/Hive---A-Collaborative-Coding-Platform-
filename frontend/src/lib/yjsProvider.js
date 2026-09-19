import * as Y from 'yjs'
import { parseFrame, roomDestinations, roomTopics } from './stomp'

const PALETTE = ['#f59e0b', '#38bdf8', '#a78bfa', '#34d399', '#fb7185', '#facc15', '#22d3ee', '#c084fc']

/** Cursor colours are derived from the user id so a peer looks the same in every room. */
export function colorForUser(userId) {
  // Number() yields NaN rather than null for a bad id, so coerce through || not ??.
  return PALETTE[Math.abs(Number(userId) || 0) % PALETTE.length]
}

function encodeBase64(bytes) {
  let binary = ''
  const chunk = 0x8000
  for (let index = 0; index < bytes.length; index += chunk) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunk))
  }
  return btoa(binary)
}

function decodeBase64(value) {
  if (!value) return new Uint8Array()
  const binary = atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

const encoder = new TextEncoder()
const decoder = new TextDecoder()

/**
 * Carries the shared document over the room's STOMP connection: Yjs updates, the sync handshake that
 * lets a late joiner catch up, and lightweight awareness (cursor + selection) heartbeats.
 *
 * The server is a dumb, authenticated relay — it never decodes CRDT bytes. So a client that joins
 * after someone has already typed cannot rebuild the document from the text snapshot alone (that
 * would fork CRDT history and duplicate text on merge); instead peers exchange state:
 *   1. broadcast {kind:'req'}                          — "whoever has state, send it to me";
 *   2. reply     {kind:'res', target:requesterId}      — our full encoded state.
 * Every peer answers; applying the same state twice is safe because Yjs updates are idempotent and
 * commutative. Awareness payloads are JSON, base64-encoded so the relay sees one opaque string.
 */
export class StompYjsProvider {
  constructor({ client, roomId, doc, canEdit, userId, username, onAwareness }) {
    this.client = client
    this.roomId = roomId
    this.doc = doc
    this.canEdit = canEdit
    this.userId = userId
    this.username = username
    this.onAwareness = onAwareness
    this.subscription = null
    this.destroyed = false
    this.peers = new Map()
    this.syncTimer = null
    this.awarenessTimer = null
    this.local = { name: username, color: colorForUser(userId), cursor: null, selection: null }

    this.handleLocalUpdate = this.handleLocalUpdate.bind(this)
    this.connect()
  }

  connect() {
    this.subscription = this.client.subscribe(roomTopics.code(this.roomId), (frame) => {
      const message = parseFrame(frame.body)
      if (!message || message.userId === this.userId) return
      this.receive(message)
    })

    this.doc.on('update', this.handleLocalUpdate)
    this.requestSync()

    // Awareness is ephemeral, so a periodic heartbeat is enough to keep the online list honest;
    // stale peers are dropped after three missed intervals.
    this.awarenessTimer = setInterval(() => this.publishAwareness(), 2500)
    this.publishAwareness()
  }

  receive(message) {
    const payload = decodeBase64(message.update)

    switch (message.kind) {
      case 'update':
        Y.applyUpdate(this.doc, payload, this)
        break
      case 'req':
        if (message.target == null || message.target === this.userId) {
          this.send('res', message.userId, Y.encodeStateAsUpdate(this.doc))
        }
        break
      case 'res':
        // Only the peer that asked applies a reply addressed to it.
        if (message.target === this.userId) Y.applyUpdate(this.doc, payload, this)
        break
      case 'awareness':
        this.receiveAwareness(message.userId, message.username, payload)
        break
      default:
        break
    }
  }

  receiveAwareness(userId, username, payload) {
    try {
      const state = JSON.parse(decoder.decode(payload))
      this.peers.set(userId, { ...state, userId, username, seenAt: Date.now() })
      this.emitAwareness()
    } catch {
      // A malformed heartbeat only costs a cursor blink; never break the document.
    }
  }

  /** Publishes this client's cursor/selection. Sent by members of every role — watching is allowed. */
  publishAwareness(cursor = this.local.cursor, selection = this.local.selection) {
    this.local = { ...this.local, cursor, selection }
    if (!this.client.connected) return
    this.send('awareness', null, encoder.encode(JSON.stringify({ cursor, selection, name: this.username })))
  }

  /** Announces that we need state, and re-asks once in case no peer answered the first attempt. */
  requestSync() {
    this.send('req', null, Y.encodeStateAsUpdate(this.doc))
    clearTimeout(this.syncTimer)
    this.syncTimer = setTimeout(() => {
      if (!this.destroyed) this.send('req', null, Y.encodeStateAsUpdate(this.doc))
    }, 1500)
  }

  handleLocalUpdate(update, origin) {
    // Never echo an update that arrived from a peer.
    if (origin === this || this.destroyed || update.length === 0) return
    // Only writers may publish; the server rejects a VIEWER edit outright.
    if (this.canEdit) this.send('update', null, update)
  }

  send(kind, target, bytes) {
    if (!this.client.connected || !bytes || bytes.length === 0) return
    const destination =
      kind === 'update' ? roomDestinations.edit(this.roomId) : roomDestinations.sync(this.roomId)
    this.client.publish({ destination, body: JSON.stringify({ kind, target, update: encodeBase64(bytes) }) })
  }

  emitAwareness() {
    const cutoff = Date.now() - 8000
    const alive = [...this.peers.values()].filter((peer) => peer.seenAt > cutoff)
    this.peers = new Map(alive.map((peer) => [peer.userId, peer]))
    this.onAwareness?.(alive)
  }

  destroy() {
    this.destroyed = true
    clearTimeout(this.syncTimer)
    clearInterval(this.awarenessTimer)
    this.doc.off('update', this.handleLocalUpdate)
    this.subscription?.unsubscribe()
    this.subscription = null
    this.peers.clear()
  }
}
