"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { RefreshCcw, Loader2 } from "lucide-react"
import { toast } from "sonner"
import { getLeaderboard } from "@/lib/api"

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
  teamId: string
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
    fetchLeaderboardData()
  }, [])

  const fetchLeaderboardData = async () => {
    setLoading(true)
    try {
      const response = await getLeaderboard()
      if (response.status === 200 && response.challenges && response.teamSolves) {
        setTeams(response.teamSolves)
        setChallenges(response.challenges)
        setIsFinalSubmission(response.competitionEnded || false)
        
      } else {
        toast("Error!", {
          description: "Failed to load leaderboard data",
        })
      }
    } catch (error) {
      console.error("Failed to fetch leaderboard data:", error)
      toast("Error", {
        description: "An unexpected error occurred while loading the leaderboard",
      })
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
        {loading ? (
            <div className="flex justify-center items-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <span className="ml-2">Loading leaderboard...</span>
            </div>
          ) : (
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
                    <TableRow key={team.teamId}>
                      <TableCell className="font-medium">{index + 1}</TableCell>
                      <TableCell>{team.name}</TableCell>
                      <TableCell className="text-right font-bold">{team.totalPoints}</TableCell>
                      {challenges.map((challenge) => {
                        const solve = team.solves.find((s) => s.challengeId === challenge.id)
                        return (
                          <TableCell key={`${team.teamId}-${challenge.id}`} className="text-center">
                            {solve?.solved ? (
                              <div className="flex flex-col items-center">
                                <Badge variant="default" className="bg-green-500 hover:bg-green-600">
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
          )}
        </CardContent>
      </Card>
    </div>
  )
}
