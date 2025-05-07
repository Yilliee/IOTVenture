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
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, MessageSquare, Trash, Clock } from "lucide-react"
import { toast } from "sonner"
import { Badge } from "@/components/ui/badge"

interface Message {
  id: string
  content: string
  teamId: string | "all"
  teamName: string | "All Teams"
  sentAt: string
  status: "pending" | "delivered" | "failed"
  deliveredCount?: number
  totalCount?: number
}

interface Team {
  id: string
  name: string
}

export default function MessagesPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "msg1",
      content: "Welcome to the IoT Venture Challenge! The competition starts in 30 minutes.",
      teamId: "all",
      teamName: "All Teams",
      sentAt: "2023-05-15 09:30",
      status: "delivered",
      deliveredCount: 16,
      totalCount: 16,
    },
    {
      id: "msg2",
      content: "Reminder: Challenge 'Find the Beacon' is now active in the north area.",
      teamId: "all",
      teamName: "All Teams",
      sentAt: "2023-05-15 10:00",
      status: "delivered",
      deliveredCount: 15,
      totalCount: 16,
    },
    {
      id: "msg3",
      content: "Your team is doing great! Keep up the good work on the signal decoding challenge.",
      teamId: "1",
      teamName: "Tech Wizards",
      sentAt: "2023-05-15 11:15",
      status: "delivered",
      deliveredCount: 5,
      totalCount: 5,
    },
    {
      id: "msg4",
      content: "Hint for your current challenge: Look for patterns in the frequency shifts.",
      teamId: "2",
      teamName: "Binary Bandits",
      sentAt: "2023-05-15 12:30",
      status: "pending",
      deliveredCount: 5,
      totalCount: 7,
    },
  ])

  const [teams, setTeams] = useState<Team[]>([
    { id: "1", name: "Tech Wizards" },
    { id: "2", name: "Binary Bandits" },
    { id: "3", name: "Circuit Breakers" },
  ])

  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [newMessage, setNewMessage] = useState({
    content: "",
    teamId: "",
  })

  const handleSendMessage = () => {
    if (!newMessage.content.trim()) {
      toast("Error", {
        description: "Message content cannot be empty",
      })
      return
    }

    if (!newMessage.teamId) {
      toast("Error", {
        description: "Please select a team",
      })
      return
    }

    const teamName =
      newMessage.teamId === "all" ? "All Teams" : teams.find((t) => t.id === newMessage.teamId)?.name || "Unknown Team"

    const newMessageObj: Message = {
      id: `msg${messages.length + 1}`,
      content: newMessage.content,
      teamId: newMessage.teamId,
      teamName,
      sentAt: new Date()
        .toLocaleString("en-US", {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
        })
        .replace(",", ""),
      status: "pending",
      deliveredCount: 0,
      totalCount: newMessage.teamId === "all" ? 16 : newMessage.teamId === "1" ? 5 : newMessage.teamId === "2" ? 7 : 4,
    }

    setMessages([newMessageObj, ...messages])
    setNewMessage({
      content: "",
      teamId: "",
    })
    setIsDialogOpen(false)

    toast("Message sent", {
      description: `Message has been queued for delivery to ${teamName}.`,
    })

    // Simulate message delivery after a delay
    setTimeout(() => {
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === newMessageObj.id
            ? {
                ...msg,
                status: "delivered",
                deliveredCount: msg.totalCount,
              }
            : msg,
        ),
      )
    }, 5000)
  }

  const handleDeleteMessage = (id: string) => {
    setMessages(messages.filter((message) => message.id !== id))
    toast("Message deleted", {
      description: "The message has been deleted from the system.",
    })
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Messages</CardTitle>
            <CardDescription>Send and manage messages to teams</CardDescription>
          </div>
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                New Message
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Send New Message</DialogTitle>
                <DialogDescription>
                  Create a new message to send to teams. Messages will be delivered to all devices in the selected team.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid gap-2">
                  <Label htmlFor="team">Select Team</Label>
                  <Select
                    onValueChange={(value) => setNewMessage({ ...newMessage, teamId: value })}
                    value={newMessage.teamId}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select a team" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Teams</SelectItem>
                      {teams.map((team) => (
                        <SelectItem key={team.id} value={team.id}>
                          {team.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="message">Message</Label>
                  <Textarea
                    id="message"
                    value={newMessage.content}
                    onChange={(e) => setNewMessage({ ...newMessage, content: e.target.value })}
                    placeholder="Enter your message here"
                    rows={5}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleSendMessage}>Send Message</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Message</TableHead>
                  <TableHead>Team</TableHead>
                  <TableHead>Sent At</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {messages.map((message) => (
                  <TableRow key={message.id}>
                    <TableCell className="max-w-[300px] truncate">
                      <div className="flex items-start gap-2">
                        <MessageSquare className="h-4 w-4 mt-1 text-primary shrink-0" />
                        <span className="truncate" title={message.content}>
                          {message.content}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell>{message.teamName}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3 text-muted-foreground" />
                        <span>{message.sentAt}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      {message.status === "delivered" ? (
                        <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                          Delivered ({message.deliveredCount}/{message.totalCount})
                        </Badge>
                      ) : message.status === "pending" ? (
                        <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200">
                          Pending ({message.deliveredCount}/{message.totalCount})
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200">
                          Failed
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="outline" size="icon" onClick={() => handleDeleteMessage(message.id)}>
                        <Trash className="h-4 w-4" />
                        <span className="sr-only">Delete Message</span>
                      </Button>
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
