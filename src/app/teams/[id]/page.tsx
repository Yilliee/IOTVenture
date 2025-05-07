"use client"

import { useState, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { toast } from "sonner"
import { Users, Save, Trash, CheckCircle, AlertTriangle } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface TeamMember {
  id: string
  username: string
  registeredAt: string
  lastActive: string
}

interface TeamChallenge {
  id: string
  name: string
  shortName: string
  solvedAt: string | null
  solvedBy: string | null
}

interface DeviceSubmission {
  id: string
  username: string
  lastActive: string
  hasSubmitted: boolean
}

export default function TeamDetailsPage() {
  const params = useParams()
  const router = useRouter()
  const teamId = params.id as string

  const [teamName, setTeamName] = useState("Loading...")
  const [maxMembers, setMaxMembers] = useState(8)
  const [members, setMembers] = useState<TeamMember[]>([])
  const [challenges, setChallenges] = useState<TeamChallenge[]>([])
  const [devices, setDevices] = useState<DeviceSubmission[]>([])
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [selectedDevice, setSelectedDevice] = useState<DeviceSubmission | null>(null)

  useEffect(() => {
    // In a real app, this would be an API call
    fetchTeamData()
  }, [teamId])

  const fetchTeamData = async () => {
    setLoading(true)
    try {
      // Mock data - would be replaced with a real fetch in production
      await new Promise((resolve) => setTimeout(resolve, 1000))

      // Different mock data based on team ID
      if (teamId === "1") {
        setTeamName("Tech Wizards")
        setMaxMembers(8)
        setMembers([
          { id: "m1", username: "wizard1", registeredAt: "2023-05-10", lastActive: "2023-05-15" },
          { id: "m2", username: "wizard2", registeredAt: "2023-05-10", lastActive: "2023-05-15" },
          { id: "m3", username: "wizard3", registeredAt: "2023-05-11", lastActive: "2023-05-14" },
          { id: "m4", username: "wizard4", registeredAt: "2023-05-12", lastActive: "2023-05-15" },
          { id: "m5", username: "wizard5", registeredAt: "2023-05-13", lastActive: "2023-05-13" },
        ])
        setChallenges([
          {
            id: "ch1",
            name: "Find the Beacon",
            shortName: "Beacon",
            solvedAt: "2023-05-15 14:30",
            solvedBy: "wizard2",
          },
          {
            id: "ch2",
            name: "Decode the Signal",
            shortName: "Signal",
            solvedAt: "2023-05-15 16:45",
            solvedBy: "wizard1",
          },
          { id: "ch3", name: "Capture the Flag", shortName: "CTF", solvedAt: "2023-05-15 18:20", solvedBy: "wizard4" },
          { id: "ch4", name: "Hack the Device", shortName: "Hack", solvedAt: null, solvedBy: null },
          { id: "ch5", name: "Solve the Puzzle", shortName: "Puzzle", solvedAt: null, solvedBy: null },
        ])
        setDevices([
          { id: "d1", username: "wizard1", lastActive: "2023-05-15 15:30", hasSubmitted: true },
          { id: "d2", username: "wizard2", lastActive: "2023-05-15 15:45", hasSubmitted: true },
          { id: "d3", username: "wizard3", lastActive: "2023-05-15 14:20", hasSubmitted: false },
          { id: "d4", username: "wizard4", lastActive: "2023-05-15 15:10", hasSubmitted: true },
          { id: "d5", username: "wizard5", lastActive: "2023-05-15 13:55", hasSubmitted: false },
        ])
      } else if (teamId === "2") {
        setTeamName("Binary Bandits")
        setMaxMembers(8)
        setMembers([
          { id: "m6", username: "bandit1", registeredAt: "2023-05-11", lastActive: "2023-05-15" },
          { id: "m7", username: "bandit2", registeredAt: "2023-05-11", lastActive: "2023-05-15" },
          { id: "m8", username: "bandit3", registeredAt: "2023-05-11", lastActive: "2023-05-14" },
          { id: "m9", username: "bandit4", registeredAt: "2023-05-12", lastActive: "2023-05-15" },
          { id: "m10", username: "bandit5", registeredAt: "2023-05-12", lastActive: "2023-05-15" },
          { id: "m11", username: "bandit6", registeredAt: "2023-05-13", lastActive: "2023-05-14" },
          { id: "m12", username: "bandit7", registeredAt: "2023-05-14", lastActive: "2023-05-15" },
        ])
        setChallenges([
          {
            id: "ch1",
            name: "Find the Beacon",
            shortName: "Beacon",
            solvedAt: "2023-05-15 15:10",
            solvedBy: "bandit3",
          },
          {
            id: "ch2",
            name: "Decode the Signal",
            shortName: "Signal",
            solvedAt: "2023-05-15 17:25",
            solvedBy: "bandit5",
          },
          { id: "ch3", name: "Capture the Flag", shortName: "CTF", solvedAt: null, solvedBy: null },
          { id: "ch4", name: "Hack the Device", shortName: "Hack", solvedAt: "2023-05-15 19:40", solvedBy: "bandit1" },
          { id: "ch5", name: "Solve the Puzzle", shortName: "Puzzle", solvedAt: null, solvedBy: null },
        ])
        setDevices([
          { id: "d6", username: "bandit1", lastActive: "2023-05-15 15:30", hasSubmitted: true },
          { id: "d7", username: "bandit2", lastActive: "2023-05-15 15:45", hasSubmitted: false },
          { id: "d8", username: "bandit3", lastActive: "2023-05-15 14:20", hasSubmitted: true },
          { id: "d9", username: "bandit4", lastActive: "2023-05-15 15:10", hasSubmitted: false },
          { id: "d10", username: "bandit5", lastActive: "2023-05-15 13:55", hasSubmitted: true },
          { id: "d11", username: "bandit6", lastActive: "2023-05-15 12:30", hasSubmitted: false },
          { id: "d12", username: "bandit7", lastActive: "2023-05-15 14:15", hasSubmitted: true },
        ])
      } else {
        setTeamName("Circuit Breakers")
        setMaxMembers(8)
        setMembers([
          { id: "m13", username: "breaker1", registeredAt: "2023-05-12", lastActive: "2023-05-15" },
          { id: "m14", username: "breaker2", registeredAt: "2023-05-12", lastActive: "2023-05-15" },
          { id: "m15", username: "breaker3", registeredAt: "2023-05-13", lastActive: "2023-05-14" },
          { id: "m16", username: "breaker4", registeredAt: "2023-05-14", lastActive: "2023-05-15" },
        ])
        setChallenges([
          {
            id: "ch1",
            name: "Find the Beacon",
            shortName: "Beacon",
            solvedAt: "2023-05-15 14:50",
            solvedBy: "breaker2",
          },
          { id: "ch2", name: "Decode the Signal", shortName: "Signal", solvedAt: null, solvedBy: null },
          { id: "ch3", name: "Capture the Flag", shortName: "CTF", solvedAt: "2023-05-15 18:05", solvedBy: "breaker1" },
          { id: "ch4", name: "Hack the Device", shortName: "Hack", solvedAt: null, solvedBy: null },
          {
            id: "ch5",
            name: "Solve the Puzzle",
            shortName: "Puzzle",
            solvedAt: "2023-05-15 20:15",
            solvedBy: "breaker4",
          },
        ])
        setDevices([
          { id: "d13", username: "breaker1", lastActive: "2023-05-15 15:30", hasSubmitted: true },
          { id: "d14", username: "breaker2", lastActive: "2023-05-15 15:45", hasSubmitted: true },
          { id: "d15", username: "breaker3", lastActive: "2023-05-15 14:20", hasSubmitted: false },
          { id: "d16", username: "breaker4", lastActive: "2023-05-15 15:10", hasSubmitted: true },
        ])
      }
    } catch (error) {
      console.error("Failed to fetch team data:", error)
      toast({
        title: "Error",
        description: "Failed to load team data. Please try again.",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  const handleUpdateMaxMembers = () => {
    // In a real app, this would call an API to update the max members
    toast({
      title: "Settings updated",
      description: `Maximum members for team "${teamName}" updated to ${maxMembers}.`,
    })
  }

  const handleRemoveMember = (memberId: string, username: string) => {
    setMembers(members.filter((member) => member.id !== memberId))
    setDevices(devices.filter((device) => device.username !== username))
    toast({
      title: "Member removed",
      description: `User "${username}" has been removed from the team.`,
    })
  }

  const handleForceSubmit = (device: DeviceSubmission) => {
    setSelectedDevice(device)
    setIsDialogOpen(true)
  }

  const confirmForceSubmit = () => {
    if (!selectedDevice) return

    // Update the device's submission status
    setDevices(
      devices.map((device) =>
        device.id === selectedDevice.id
          ? {
              ...device,
              hasSubmitted: true,
            }
          : device,
      ),
    )

    setIsDialogOpen(false)

    toast({
      title: "Submission forced",
      description: `Final submission for device "${selectedDevice.username}" has been forced.`,
    })
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Users className="h-6 w-6 text-primary" />
            <CardTitle>{teamName}</CardTitle>
          </div>
          <CardDescription>
            Team ID: {teamId} • {members.length} members
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="members">
            <TabsList className="mb-4">
              <TabsTrigger value="members">Members</TabsTrigger>
              <TabsTrigger value="challenges">Challenges</TabsTrigger>
              <TabsTrigger value="submissions">Submissions</TabsTrigger>
              <TabsTrigger value="settings">Settings</TabsTrigger>
            </TabsList>

            <TabsContent value="members">
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Username</TableHead>
                      <TableHead>Registered</TableHead>
                      <TableHead>Last Active</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {members.map((member) => (
                      <TableRow key={member.id}>
                        <TableCell className="font-medium">{member.username}</TableCell>
                        <TableCell>{member.registeredAt}</TableCell>
                        <TableCell>{member.lastActive}</TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="outline"
                            size="icon"
                            onClick={() => handleRemoveMember(member.id, member.username)}
                          >
                            <Trash className="h-4 w-4" />
                            <span className="sr-only">Remove Member</span>
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              <div className="mt-4 text-sm text-muted-foreground">
                {members.length} of {maxMembers} members registered
              </div>
            </TabsContent>

            <TabsContent value="challenges">
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Challenge</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Solved At</TableHead>
                      <TableHead>Solved By</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {challenges.map((challenge) => (
                      <TableRow key={challenge.id}>
                        <TableCell className="font-medium">
                          {challenge.name}
                          <span className="ml-2 text-xs text-muted-foreground">({challenge.shortName})</span>
                        </TableCell>
                        <TableCell>
                          {challenge.solvedAt ? (
                            <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                              Solved
                            </span>
                          ) : (
                            <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-800">
                              Pending
                            </span>
                          )}
                        </TableCell>
                        <TableCell>{challenge.solvedAt || "—"}</TableCell>
                        <TableCell>{challenge.solvedBy || "—"}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </TabsContent>

            <TabsContent value="submissions">
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Device</TableHead>
                      <TableHead>Last Active</TableHead>
                      <TableHead>Submission Status</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {devices.map((device) => (
                      <TableRow key={device.id}>
                        <TableCell className="font-medium">{device.username}</TableCell>
                        <TableCell>{device.lastActive}</TableCell>
                        <TableCell>
                          {device.hasSubmitted ? (
                            <div className="flex items-center gap-1">
                              <CheckCircle className="h-4 w-4 text-green-500" />
                              <span>Submitted</span>
                            </div>
                          ) : (
                            <div className="flex items-center gap-1">
                              <AlertTriangle className="h-4 w-4 text-yellow-500" />
                              <span>Pending</span>
                            </div>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleForceSubmit(device)}
                            disabled={device.hasSubmitted}
                          >
                            Force Submit
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              <div className="mt-4 text-sm text-muted-foreground">
                {devices.filter((d) => d.hasSubmitted).length} of {devices.length} devices have submitted
              </div>
            </TabsContent>

            <TabsContent value="settings">
              <Card>
                <CardHeader>
                  <CardTitle>Team Settings</CardTitle>
                  <CardDescription>Configure team-specific settings</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="max-members">Maximum Members</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        id="max-members"
                        type="number"
                        value={maxMembers}
                        onChange={(e) => setMaxMembers(Number.parseInt(e.target.value))}
                        min="1"
                        className="w-24"
                      />
                      <Button onClick={handleUpdateMaxMembers}>
                        <Save className="mr-2 h-4 w-4" />
                        Save
                      </Button>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      This setting controls how many members can register for this team.
                    </p>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Force Final Submission</DialogTitle>
            <DialogDescription>
              Are you sure you want to force final submission for device "{selectedDevice?.username}"? This will use the
              last data received from this device.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <p className="text-sm text-muted-foreground">
              Device: <span className="font-medium text-foreground">{selectedDevice?.username}</span>
            </p>
            <p className="text-sm text-muted-foreground">
              Last active: <span className="font-medium text-foreground">{selectedDevice?.lastActive}</span>
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
