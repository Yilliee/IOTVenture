"use client"

import { useState } from "react"
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
import { Plus, RefreshCw, Trash, Eye } from "lucide-react"
import Link from "next/link"
import { toast } from "sonner"

interface Team {
  id: string
  name: string
  memberCount: number
  maxMembers: number
  createdAt: string
}

export default function TeamsPage() {
  const [teams, setTeams] = useState<Team[]>([
    { id: "1", name: "Tech Wizards", memberCount: 5, maxMembers: 8, createdAt: "2023-05-10" },
    { id: "2", name: "Binary Bandits", memberCount: 7, maxMembers: 8, createdAt: "2023-05-11" },
    { id: "3", name: "Circuit Breakers", memberCount: 4, maxMembers: 8, createdAt: "2023-05-12" },
  ])
  const [newTeamName, setNewTeamName] = useState("")
  const [newTeamMaxMembers, setNewTeamMaxMembers] = useState("8")
  const [isDialogOpen, setIsDialogOpen] = useState(false)

  const handleCreateTeam = () => {
    if (!newTeamName.trim()) {
      toast({
        title: "Error",
        description: "Team name cannot be empty",
        variant: "destructive",
      })
      return
    }

    const maxMembers = Number.parseInt(newTeamMaxMembers)
    if (isNaN(maxMembers) || maxMembers <= 0) {
      toast({
        title: "Error",
        description: "Max members must be a positive number",
        variant: "destructive",
      })
      return
    }

    const newTeam: Team = {
      id: (teams.length + 1).toString(),
      name: newTeamName,
      memberCount: 0,
      maxMembers,
      createdAt: new Date().toISOString().split("T")[0],
    }

    setTeams([...teams, newTeam])
    setNewTeamName("")
    setNewTeamMaxMembers("8")
    setIsDialogOpen(false)

    toast({
      title: "Team created",
      description: `Team "${newTeamName}" has been created successfully.`,
    })
  }

  const handleResetPassword = (teamId: string, teamName: string) => {
    // In a real app, this would call an API to reset the password
    toast({
      title: "Password reset",
      description: `Password for team "${teamName}" has been reset. New password: TeamPass123`,
    })
  }

  const handleDeleteTeam = (teamId: string, teamName: string) => {
    setTeams(teams.filter((team) => team.id !== teamId))
    toast({
      title: "Team deleted",
      description: `Team "${teamName}" has been deleted successfully.`,
    })
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
                <Button onClick={handleCreateTeam}>Create Team</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardHeader>
        <CardContent>
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
        </CardContent>
      </Card>
    </div>
  )
}
