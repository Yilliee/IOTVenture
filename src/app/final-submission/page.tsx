"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Progress } from "@/components/ui/progress"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { AlertTriangle, CheckCircle, RefreshCw, Trophy, Download } from "lucide-react"
import { toast } from "sonner"

interface TeamSubmission {
  id: string
  name: string
  totalDevices: number
  submittedDevices: number
  lastUpdate: string
  status: "pending" | "partial" | "complete"
}

export default function FinalSubmissionPage() {
  const [teams, setTeams] = useState<TeamSubmission[]>([
    {
      id: "1",
      name: "Tech Wizards",
      totalDevices: 5,
      submittedDevices: 3,
      lastUpdate: "2023-05-15 15:45",
      status: "partial",
    },
    {
      id: "2",
      name: "Binary Bandits",
      totalDevices: 7,
      submittedDevices: 7,
      lastUpdate: "2023-05-15 15:30",
      status: "complete",
    },
    {
      id: "3",
      name: "Circuit Breakers",
      totalDevices: 4,
      submittedDevices: 0,
      lastUpdate: "N/A",
      status: "pending",
    },
  ])

  const [selectedTeam, setSelectedTeam] = useState<TeamSubmission | null>(null)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isContestEnded, setIsContestEnded] = useState(false)
  const [isGeneratingResults, setIsGeneratingResults] = useState(false)

  const totalDevices = teams.reduce((sum, team) => sum + team.totalDevices, 0)
  const submittedDevices = teams.reduce((sum, team) => sum + team.submittedDevices, 0)
  const submissionPercentage = Math.round((submittedDevices / totalDevices) * 100)

  const handleForceSubmit = (teamId: string) => {
    const team = teams.find((t) => t.id === teamId)
    if (!team) return

    setSelectedTeam(team)
    setIsDialogOpen(true)
  }

  const confirmForceSubmit = () => {
    if (!selectedTeam) return

    // Update the team's submission status
    setTeams(
      teams.map((team) =>
        team.id === selectedTeam.id
          ? {
              ...team,
              submittedDevices: team.totalDevices,
              lastUpdate: new Date()
                .toLocaleString("en-US", {
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                  hour12: false,
                })
                .replace(",", ""),
              status: "complete",
            }
          : team,
      ),
    )

    setIsDialogOpen(false)

    toast("Submission forced", {
      description: `Final submission for team "${selectedTeam.name}" has been forced.`,
    })
  }

  const handleEndContest = () => {
    setIsContestEnded(true)

    toast("Contest ended", {
      description: "The contest has been marked as ended. Teams can no longer submit new solutions.",
    })
  }

  const handleGenerateResults = () => {
    setIsGeneratingResults(true)

    // Simulate generating results
    setTimeout(() => {
      setIsGeneratingResults(false)

      toast("Results generated", {
        description: "Final results have been generated and are ready for download.",
      })
    }, 3000)
  }

  return (
    <div className="container py-8">
      <Card className="mb-6">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Trophy className="h-6 w-6 text-primary" />
            <CardTitle>Final Submission Status</CardTitle>
          </div>
          <CardDescription>Track and manage final submissions from all teams</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6">
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Overall Submission Progress</span>
                <span className="text-sm text-muted-foreground">
                  {submittedDevices} of {totalDevices} devices
                </span>
              </div>
              <Progress value={submissionPercentage} className="h-2" />
            </div>

            <div className="flex flex-col sm:flex-row gap-4 justify-between">
              <div className="flex items-center gap-2">
                <Badge variant={isContestEnded ? "destructive" : "outline"} className="text-xs">
                  {isContestEnded ? "Contest Ended" : "Contest Active"}
                </Badge>
                {!isContestEnded && (
                  <Button variant="outline" size="sm" onClick={handleEndContest}>
                    End Contest
                  </Button>
                )}
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={isGeneratingResults || !isContestEnded}
                  onClick={handleGenerateResults}
                >
                  {isGeneratingResults ? (
                    <>
                      <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                      Generating...
                    </>
                  ) : (
                    <>
                      <Trophy className="mr-2 h-4 w-4" />
                      Generate Results
                    </>
                  )}
                </Button>

                <Button size="sm" disabled={isGeneratingResults || !isContestEnded}>
                  <Download className="mr-2 h-4 w-4" />
                  Download Results
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Team Submissions</CardTitle>
          <CardDescription>Status of final submissions from each team</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Team</TableHead>
                  <TableHead>Devices</TableHead>
                  <TableHead>Submission Status</TableHead>
                  <TableHead>Last Update</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {teams.map((team) => (
                  <TableRow key={team.id}>
                    <TableCell className="font-medium">{team.name}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <span>
                          {team.submittedDevices}/{team.totalDevices}
                        </span>
                        <Progress value={(team.submittedDevices / team.totalDevices) * 100} className="h-2 w-20" />
                      </div>
                    </TableCell>
                    <TableCell>
                      {team.status === "complete" ? (
                        <div className="flex items-center gap-1">
                          <CheckCircle className="h-4 w-4 text-green-500" />
                          <span>Complete</span>
                        </div>
                      ) : team.status === "partial" ? (
                        <div className="flex items-center gap-1">
                          <AlertTriangle className="h-4 w-4 text-yellow-500" />
                          <span>Partial</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1">
                          <AlertTriangle className="h-4 w-4 text-gray-400" />
                          <span>Pending</span>
                        </div>
                      )}
                    </TableCell>
                    <TableCell>{team.lastUpdate}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleForceSubmit(team.id)}
                        disabled={team.status === "complete" || !isContestEnded}
                      >
                        Force Submit
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Force Final Submission</DialogTitle>
            <DialogDescription>
              Are you sure you want to force final submission for team "{selectedTeam?.name}"? This will use the last
              data received from all devices.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <p className="text-sm text-muted-foreground">
              Team: <span className="font-medium text-foreground">{selectedTeam?.name}</span>
            </p>
            <p className="text-sm text-muted-foreground">
              Current submission:{" "}
              <span className="font-medium text-foreground">
                {selectedTeam?.submittedDevices}/{selectedTeam?.totalDevices} devices
              </span>
            </p>
            <p className="text-sm text-muted-foreground mt-4">
              <AlertTriangle className="h-4 w-4 text-yellow-500 inline mr-1" />
              This action cannot be undone.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={confirmForceSubmit}>
              Force Submit
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
