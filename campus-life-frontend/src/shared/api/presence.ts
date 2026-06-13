import http from './http'

export async function heartbeatPresence(portal: string) {
  const { data } = await http.post('/presence/heartbeat', { portal })
  return data
}

export async function offlinePresence(portal: string) {
  const { data } = await http.post('/presence/offline', { portal })
  return data
}
