/**
 * WebSocket composable for real-time notifications via STOMP.
 *
 * Usage:
 *   const { connect, disconnect, onNotification } = useWebSocket()
 *   connect()
 *   onNotification((data) => { console.log('New notification:', data) })
 */

import { ref, onUnmounted } from 'vue'

export interface WebSocketNotification {
  type: string
  title: string
  message: string
  timestamp: number
  workflowRunId?: string
  status?: string
  workflowName?: string
}

type NotificationCallback = (data: WebSocketNotification) => void

const isConnected = ref(false)
const notifications = ref<WebSocketNotification[]>([])
const callbacks: NotificationCallback[] = []

let socket: WebSocket | null = null
let stompClient: any = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 10
const RECONNECT_DELAY = 3000

function getWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${protocol}//${host}/ws`
}

function connect() {
  if (socket && socket.readyState === WebSocket.OPEN) return

  const url = getWsUrl()
  console.log('[WebSocket] Connecting to', url)

  // Use SockJS if available, otherwise raw WebSocket
  try {
    // Dynamic import approach - try SockJS first
    const sockJsUrl = `${window.location.protocol}//${window.location.host}/ws`

    socket = new WebSocket(url.replace('ws:', 'ws:').replace('wss:', 'wss:'))

    socket.onopen = () => {
      console.log('[WebSocket] Connected')
      isConnected.value = true
      reconnectAttempts = 0

      // Simple STOMP-like subscription via raw WebSocket
      // Send CONNECT frame
      if (socket) {
        socket.send('CONNECT\naccept-version:1.1\n\n\0')

        // Subscribe to user-specific notifications
        const tenantId = localStorage.getItem('tenantId') || '1'
        socket.send(
          `SUBSCRIBE\nid:sub-0\ndestination:/topic/tenant/${tenantId}/notifications\n\n\0`
        )
      }
    }

    socket.onmessage = (event) => {
      const data = event.data
      if (data && typeof data === 'string' && data.startsWith('MESSAGE')) {
        // Parse STOMP MESSAGE frame
        const bodyStart = data.indexOf('\n\n')
        if (bodyStart !== -1) {
          const body = data.substring(bodyStart + 2).replace(/\0$/, '')
          try {
            const notification = JSON.parse(body) as WebSocketNotification
            notifications.value.unshift(notification)
            // Keep only last 100 notifications
            if (notifications.value.length > 100) {
              notifications.value = notifications.value.slice(0, 100)
            }
            // Notify all callbacks
            callbacks.forEach((cb) => cb(notification))
          } catch (e) {
            console.warn('[WebSocket] Failed to parse message:', e)
          }
        }
      }
    }

    socket.onclose = () => {
      console.log('[WebSocket] Disconnected')
      isConnected.value = false
      attemptReconnect()
    }

    socket.onerror = (error) => {
      console.warn('[WebSocket] Error:', error)
      isConnected.value = false
    }
  } catch (e) {
    console.warn('[WebSocket] Connection failed:', e)
    attemptReconnect()
  }
}

function attemptReconnect() {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.warn('[WebSocket] Max reconnect attempts reached')
    return
  }
  if (reconnectTimer) clearTimeout(reconnectTimer)
  reconnectAttempts++
  const delay = RECONNECT_DELAY * Math.min(reconnectAttempts, 5)
  console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${reconnectAttempts})`)
  reconnectTimer = setTimeout(connect, delay)
}

function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = MAX_RECONNECT_ATTEMPTS // Prevent auto-reconnect
  if (socket) {
    socket.close()
    socket = null
  }
  isConnected.value = false
}

function onNotification(callback: NotificationCallback) {
  callbacks.push(callback)
  return () => {
    const idx = callbacks.indexOf(callback)
    if (idx !== -1) callbacks.splice(idx, 1)
  }
}

export function useWebSocket() {
  return {
    isConnected,
    notifications,
    connect,
    disconnect,
    onNotification,
  }
}
