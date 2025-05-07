/**
 * API utility functions for making requests to the server
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

/**
 * Generic fetch wrapper that returns response code and data
 */
async function fetchAPI<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<{ status: number; data: T | null; error?: string }> {
  const url = `${API_BASE_URL}${endpoint}`

  const defaultOptions: RequestInit = {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  }

  try {
    const response = await fetch(url, { ...defaultOptions, ...options })

    // For 204 No Content responses
    if (response.status === 204) {
      return { status: response.status, data: null }
    }

    let data = null
    let error = null

    try {
      // Only try to parse JSON if there's content
      if (response.headers.get("content-length") !== "0") {
        data = await response.json()
      }
    } catch (e) {
      error = "Failed to parse response"
    }

    return {
      status: response.status,
      data,
      error: !response.ok ? data?.error || `HTTP error ${response.status} with error ${error}` : null,
    }
  } catch (e) {
    return {
      status: 0,
      data: null,
      error: e instanceof Error ? e.message : "Network error",
    }
  }
}

export interface Challenge {
  id: string
  name: string
  shortName: string
  points: number
  location: {
    topLeft: { lat: number; lng: number }
    bottomRight: { lat: number; lng: number }
  }
  keyHash: string
}

// export interface SerialPort {
//   path: string
//   manufacturer: string
//   serialNumber: string
// }

// // Serial port API
// export async function getSerialPorts(): Promise<{ status: number; ports: SerialPort[]; error?: string }> {
//   const response = await fetchAPI<{ ports: SerialPort[] }>("/admin/serial-ports")
//   return {
//     status: response.status,
//     ports: response.data?.ports || [],
//     error: response.error,
//   }
// }

// export async function readNfcTag(portPath: string): Promise<{ status: number; keyHash?: string; error?: string }> {
//   const response = await fetchAPI<{ success: boolean; keyHash: string }>("/admin/read-nfc", {
//     method: "POST",
//     body: JSON.stringify({ portPath }),
//   })
//   return {
//     status: response.status,
//     keyHash: response.data?.keyHash,
//     error: response.error,
//   }
// }

export async function getChallenges(): Promise<{ status: number; challenges: Challenge[]; error?: string }> {
  const response = await fetchAPI<{ challenges: Challenge[] }>("/admin/challenges")  
  return {
    status: response.status,
    challenges: response.data?.challenges || [],
    error: response.error,
  }
}

export async function getChallenge(id: string): Promise<{ status: number; challenge?: Challenge; error?: string }> {
  const response = await fetchAPI<Challenge>(`/admin/challenges/${id}`)
  return {
    status: response.status,
    challenge: response?.data || undefined,
    error: response.error,
  }
}

export async function createChallenge(
  challenge: Omit<Challenge, "id">,
): Promise<{ status: number; challenge?: Challenge; error?: string }> {
  const response = await fetchAPI<Challenge>("/admin/challenges", {
    method: "POST",
    body: JSON.stringify(challenge),
  })
  return {
    status: response.status,
    challenge: response?.data || undefined,
    error: response.error,
  }
}

export async function updateChallenge(
  id: string,
  challenge: Partial<Challenge>,
): Promise<{ status: number; challenge?: Challenge; error?: string }> {
  const response = await fetchAPI<Challenge>(`/admin/challenges/${id}`, {
    method: "PUT",
    body: JSON.stringify(challenge),
  })
  return {
    status: response.status,
    challenge: response?.data || undefined,
    error: response.error,
  }
}

export async function deleteChallenge(id: string): Promise<{ status: number; error?: string }> {
  const response = await fetchAPI<void>(`/admin/challenges/${id}`, {
    method: "DELETE",
  })
  return {
    status: response.status,
    error: response.error,
  }
}

// Team API
export interface Team {
  id: string
  name: string
  maxMembers: number
  memberCount: number
  createdAt: string
}

export interface TeamMember {
  id: string
  username: string
  registeredAt: string
  lastActive: string,
  hasSubmitted: boolean
}

export async function getTeams(): Promise<{ status: number; teams: Team[]; error?: string }> {
  const response = await fetchAPI<{ teams: Team[] }>("/admin/teams")
  return {
    status: response.status,
    teams: response.data?.teams || [],
    error: response.error,
  }
}

export async function getTeam(id: string): Promise<{
  status: number
  team?: Team
  members?: TeamMember[]
  solves?: any[]
  error?: string
}> {
  const response = await fetchAPI<{ team: Team; members: TeamMember[]; solves: any[] }>(`/admin/teams/${id}`)
  return {
    status: response.status,
    team: response.data?.team,
    members: response.data?.members || [],
    solves: response.data?.solves || [],
    error: response.error,
  }
}

export async function createTeam(team: { name: string; password: string; maxMembers: number }): Promise<{
  status: number
  team?: Team
  error?: string
}> {
  const response = await fetchAPI<Team>("/admin/teams", {
    method: "POST",
    body: JSON.stringify(team),
  })
  return {
    status: response.status,
    team: response?.data || undefined,
    error: response.error,
  }
}

export async function updateTeam(
  id: string,
  data: { password?: string, maxMembers?: number },
): Promise<{ status: number; team?: Team; error?: string }> {
  const response = await fetchAPI<Team>(`/admin/teams/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  })
  return {
    status: response.status,
    team: response?.data || undefined,
    error: response.error,
  }
}

export async function deleteTeam(id: string): Promise<{ status: number; error?: string }> {
  const response = await fetchAPI<void>(`/admin/teams/${id}`, {
    method: "DELETE",
  })
  return {
    status: response.status,
    error: response.error,
  }
}

export async function resetTeamPassword(id: string): Promise<{ status: number; newPassword?: string; error?: string }> {
  const response = await fetchAPI<{ newPassword: string }>(`/admin/teams/${id}/reset-password`, {
    method: "POST",
  })
  return {
    status: response.status,
    newPassword: response.data?.newPassword,
    error: response.error,
  }
}

export async function removeTeamMember(teamId: string, memberId: string): Promise<{ status: number; error?: string }> {
  const response = await fetchAPI<void>(`/admin/teams/${teamId}/members/${memberId}`, {
    method: "DELETE",
  })
  return {
    status: response.status,
    error: response.error,
  }
}

// Message API
export interface Message {
  id: string
  content: string
  teamId: string | "all"
  teamName: string | "All Teams"
  sentAt: string
  status: "pending" | "delivered" | "failed"
  deliveredCount?: number
  totalCount?: number
}

export async function getMessages(): Promise<{ status: number; messages: Message[]; error?: string }> {
  const response = await fetchAPI<{ messages: Message[] }>("/admin/messages")
  return {
    status: response.status,
    messages: response.data?.messages || [],
    error: response.error,
  }
}

export async function sendMessage(message: { teamId: string; content: string }): Promise<{
  status: number
  message?: Message
  error?: string
}> {
  const response = await fetchAPI<{ success: boolean; messageId: string }>("/admin/send-message", {
    method: "POST",
    body: JSON.stringify(message),
  })
  return {
    status: response.status,
    message: response.data?.success ? ({ id: response.data.messageId } as any) : undefined,
    error: response.error,
  }
}

export async function deleteMessage(id: string): Promise<{ status: number; error?: string }> {
  const response = await fetchAPI<void>(`/admin/messages/${id}`, {
    method: "DELETE",
  })
  return {
    status: response.status,
    error: response.error,
  }
}

// Authentication API
export async function login(credentials: { username: string; password: string }): Promise<{
  status: number
  success?: boolean
  error?: string
}> {
  const response = await fetchAPI<{ success: boolean }>("/admin/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  })
  return {
    status: response.status,
    success: response.data?.success,
    error: response.error,
  }
}

export async function logout(): Promise<{ status: number; success?: boolean; error?: string }> {
  const response = await fetchAPI<{ success: boolean }>("/admin/logout", {
    method: "POST",
  })
  return {
    status: response.status,
    success: response.data?.success,
    error: response.error,
  }
}

// Leaderboard API
export async function getLeaderboard(): Promise<{
  status: number
  teams?: any[]
  challenges?: any[]
  teamSolves?: any[]
  competitionEnded?: boolean
  error?: string
}> {
  const response = await fetchAPI<{ challenges: any[]; teamSolves: any[], competitionEnded: any }>("/leaderboard")
  return {
    status: response.status,
    challenges: response.data?.challenges,
    teamSolves: response.data?.teamSolves,
    competitionEnded: response.data?.competitionEnded,
    error: response.error,
  }
}

// Final submission API
export async function getSubmissionStatus(): Promise<{ status: number; teams?: any[]; error?: string }> {
  const response = await fetchAPI<{ teams: any[] }>("/admin/submission-status")
  return {
    status: response.status,
    teams: response.data?.teams,
    error: response.error,
  }
}

export async function endContest(): Promise<{ status: number; success?: boolean; error?: string }> {
  const response = await fetchAPI<{ success: boolean }>("/admin/end-contest", {
    method: "POST",
  })
  return {
    status: response.status,
    success: response.data?.success,
    error: response.error,
  }
}

export async function forceSubmit(userId: string): Promise<{ status: number; success?: boolean; error?: string }> {
  const response = await fetchAPI<{ success: boolean }>(`/admin/users/${userId}/force-submit`, {
    method: "POST",
  })
  return {
    status: response.status,
    success: response.data?.success,
    error: response.error,
  }
}

export async function generateResults(): Promise<{
  status: number
  success?: boolean
  resultUrl?: string
  error?: string
}> {
  const response = await fetchAPI<{ success: boolean; resultUrl: string }>("/admin/generate-results", {
    method: "POST",
  })
  return {
    status: response.status,
    success: response.data?.success,
    resultUrl: response.data?.resultUrl,
    error: response.error,
  }
}
