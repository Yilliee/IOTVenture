"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { RefreshCcw } from "lucide-react"

interface Challenge {
  id: string
  name: string
  shortName: string
  points: number
}

interface TeamSolve {
  challengeId: string
  timestamp: number
  solved: boolean
}

interface Team {
  id: string
  name: string
  solves: TeamSolve[]
  totalPoints: number
}

export default function LeaderboardPage() {
  const [teams, setTeams] = useState<Team[]>([])
  const [challenges, setChallenges] = useState<Challenge[]>([])
  const [loading, setLoading] = useState(true)
  const [isFinalSubmission, setIsFinalSubmission] = useState(false)

  useEffect(() => {
    // In a real app, this would be an API call
    fetchLeaderboardData()
  }, [])

  const fetchLeaderboardData = async () => {
    setLoading(true)
    try {
      // Mock data - would be replaced with a real fetch in production
      await new Promise((resolve) => setTimeout(resolve, 1000))

      const mockChallenges = [
        { id: "ch1", name: "Find the Beacon", shortName: "Beacon", points: 100 },
        { id: "ch2", name: "Decode the Signal", shortName: "Signal", points: 150 },
        { id: "ch3", name: "Capture the Flag", shortName: "CTF", points: 200 },
        { id: "ch4", name: "Hack the Device", shortName: "Hack", points: 250 },
        { id: "ch5", name: "Solve the Puzzle", shortName: "Puzzle", points: 300 },
      ]

      const mockTeams = [
        {
          id: "team1",
          name: "Tech Wizards",
          solves: [
            { challengeId: "ch1", timestamp: Date.now() - 3600000, solved: true },
            { challengeId: "ch2", timestamp: Date.now() - 2400000, solved: true },
            { challengeId: "ch3", timestamp: Date.now() - 1200000, solved: true },
            { challengeId: "ch4", timestamp: 0, solved: false },
            { challengeId: "ch5", timestamp: 0, solved: false },
          ],
          totalPoints: 450,
        },
        {
          id: "team2",
          name: "Binary Bandits",
          solves: [
            { challengeId: "ch1", timestamp: Date.now() - 3200000, solved: true },
            { challengeId: "ch2", timestamp: Date.now() - 2000000, solved: true },
            { challengeId: "ch3", timestamp: 0, solved: false },
            { challengeId: "ch4", timestamp: Date.now() - 800000, solved: true },
            { challengeId: "ch5", timestamp: 0, solved: false },
          ],
          totalPoints: 500,
        },
        {
          id: "team3",
          name: "Circuit Breakers",
          solves: [
            { challengeId: "ch1", timestamp: Date.now() - 3400000, solved: true },
            { challengeId: "ch2", timestamp: 0, solved: false },
            { challengeId: "ch3", timestamp: Date.now() - 1000000, solved: true },
            { challengeId: "ch4", timestamp: 0, solved: false },
            { challengeId: "ch5", timestamp: Date.now() - 400000, solved: true },
          ],
          totalPoints: 600,
        },
      ]

      // Sort teams by total points
      const sortedTeams = [...mockTeams].sort((a, b) => b.totalPoints - a.totalPoints)

      setChallenges(mockChallenges)
      setTeams(sortedTeams)
      setIsFinalSubmission(false)
    } catch (error) {
      console.error("Failed to fetch leaderboard data:", error)
    } finally {
      setLoading(false)
    }
  }

  const formatTime = (timestamp: number) => {
    if (timestamp === 0) return "—"

    const date = new Date(timestamp)
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Leaderboard</CardTitle>
            <CardDescription>
              {isFinalSubmission
                ? "Final results of the competition"
                : "Current standings (tentative until final submission)"}
            </CardDescription>
          </div>
          <Button variant="outline" size="icon" onClick={fetchLeaderboardData} disabled={loading}>
            <RefreshCcw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            <span className="sr-only">Refresh</span>
          </Button>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[80px]">Rank</TableHead>
                  <TableHead>Team</TableHead>
                  <TableHead className="text-right">Points</TableHead>
                  {challenges.map((challenge) => (
                    <TableHead key={challenge.id} className="text-center">
                      {challenge.shortName}
                    </TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {teams.map((team, index) => (
                  <TableRow key={team.id}>
                    <TableCell className="font-medium">{index + 1}</TableCell>
                    <TableCell>{team.name}</TableCell>
                    <TableCell className="text-right font-bold">{team.totalPoints}</TableCell>
                    {challenges.map((challenge) => {
                      const solve = team.solves.find((s) => s.challengeId === challenge.id)
                      return (
                        <TableCell key={`${team.id}-${challenge.id}`} className="text-center">
                          {solve?.solved ? (
                            <div className="flex flex-col items-center">
                              <Badge variant="success" className="bg-green-500 hover:bg-green-600">
                                Solved
                              </Badge>
                              <span className="text-xs mt-1">{formatTime(solve.timestamp)}</span>
                            </div>
                          ) : (
                            "—"
                          )}
                        </TableCell>
                      )
                    })}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
