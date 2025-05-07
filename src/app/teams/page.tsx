"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Plus, RefreshCw, Trash, Eye, Loader2 } from "lucide-react"
import Link from "next/link"
import { toast } from "sonner"
import { getTeams, createTeam, deleteTeam } from "@/lib/api"

interface Team {
  id: string
  name: string
  memberCount: number
  maxMembers: number
  createdAt: string
}

export default function TeamsPage() {
  const [teams, setTeams] = useState<Team[]>([])
  const [newTeamName, setNewTeamName] = useState("")
  const [newTeamMaxMembers, setNewTeamMaxMembers] = useState("8")
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  
  useEffect(() => {
    fetchTeams()
  }, [])

  const fetchTeams = async () => {
    setLoading(true)
    try {
      const response = await getTeams()

      if (response.status === 200 && response.teams) {
        console.log("Fetched teams:", response.teams)
        setTeams(response.teams)
      } else {
        toast("Error", {
          description: response.error || "Failed to load teams",
        })
      }
    } catch (error) {
      console.error("Failed to fetch teams:", error)
      toast("Error", {
        description: "An unexpected error occurred while loading teams",
      })
    } finally {
      setLoading(false)
    }
  }

  const handleCreateTeam = async () => {
    if (!newTeamName.trim()) {
      toast("Error", { 
        description: "Team name cannot be empty",
      })
      return
    }

    const maxMembers = Number.parseInt(newTeamMaxMembers)
    if (isNaN(maxMembers) || maxMembers <= 0) {
      toast("Error", {
        description: "Max members must be a positive number",
      })
      return
    }

    const newTeamPassword = "TeamPass123" // TODO: Randomly generate a password

    setCreating(true)
    try {
      const response = await createTeam({
        name: newTeamName,
        password: newTeamPassword,
        maxMembers,
      })

      if (response.status === 201 && response.team) {
        setTeams([...teams, response.team])
        setNewTeamName("")
        setNewTeamMaxMembers("8")
        setIsDialogOpen(false)

        toast("Team created", {
          description: `Team "${newTeamName}" has been created successfully.`,
        })
      } else {
        toast("Error", {
          description: response.error || "Failed to create team",
        })
      }
    } catch (error) {
      console.error("Failed to create team:", error)
      toast("Error", {
        description: "An unexpected error occurred while creating the team",
      })
    } finally {
      setCreating(false)
    }
  }

  const handleResetPassword = (teamId: string, teamName: string) => {
    // TODO: call an API to reset the password
    toast("Password reset", {
      description: `Password for team "${teamName}" has been reset. New password: TeamPass123`,
    })
  }

  const handleDeleteTeam = async (teamId: string, teamName: string) => {
    try {
      const response = await deleteTeam(teamId)

      if (response.status === 204 || response.status === 200) {
        setTeams(teams.filter((team) => team.id !== teamId))
        toast("Team deleted", {
          description: `Team "${teamName}" has been deleted successfully.`,
        })
      } else {
        toast("Error", {
          description: response.error || "Failed to delete team",
        })
      }
    } catch (error) {
      console.error("Failed to delete team:", error)
      toast("Error", {
        description: "An unexpected error occurred while deleting the team",
      })
    }
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Team Management</CardTitle>
            <CardDescription>Create and manage teams for the competition</CardDescription>
          </div>
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Create Team
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create New Team</DialogTitle>
                <DialogDescription>
                  Add a new team to the competition. Teams can have multiple members up to the specified limit.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid gap-2">
                  <Label htmlFor="team-name">Team Name</Label>
                  <Input
                    id="team-name"
                    value={newTeamName}
                    onChange={(e) => setNewTeamName(e.target.value)}
                    placeholder="Enter team name"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="max-members">Maximum Members</Label>
                  <Input
                    id="max-members"
                    type="number"
                    value={newTeamMaxMembers}
                    onChange={(e) => setNewTeamMaxMembers(e.target.value)}
                    min="1"
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateTeam} disabled={creating}>
                  {creating ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating...
                    </>
                  ) : (
                    "Create Team"
                  )}
                </Button>
            </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardHeader>
        <CardContent>
        {loading ? (
            <div className="flex justify-center items-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <span className="ml-2">Loading teams...</span>
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Team Name</TableHead>
                    <TableHead>Members</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {teams.map((team) => (
                    <TableRow key={team.id}>
                      <TableCell className="font-medium">{team.name}</TableCell>
                      <TableCell>
                        {team.memberCount} / {team.maxMembers}
                      </TableCell>
                      <TableCell>{team.createdAt}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="icon"
                            onClick={() => handleResetPassword(team.id, team.name)}
                            title="Reset Password"
                          >
                            <RefreshCw className="h-4 w-4" />
                            <span className="sr-only">Reset Password</span>
                          </Button>
                          <Button variant="outline" size="icon" asChild title="View Team Details">
                            <Link href={`/teams/${team.id}`}>
                              <Eye className="h-4 w-4" />
                              <span className="sr-only">View Team</span>
                            </Link>
                          </Button>
                          <Button
                            variant="outline"
                            size="icon"
                            onClick={() => handleDeleteTeam(team.id, team.name)}
                            title="Delete Team"
                          >
                            <Trash className="h-4 w-4" />
                            <span className="sr-only">Delete Team</span>
                          </Button>
                        </div>
                      </TableCell>
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
