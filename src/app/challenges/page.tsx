"use client"

import type React from "react"

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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, Trash, Edit, RefreshCw } from "lucide-react"
import { toast } from "sonner"
import Link from "next/link"

interface SerialPort {
  path: string
  manufacturer: string
  serialNumber: string
}

interface Challenge {
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

export default function ChallengesPage() {
  const [challenges, setChallenges] = useState<Challenge[]>([
    {
      id: "ch1",
      name: "Find the Beacon",
      shortName: "Beacon",
      points: 100,
      location: {
        topLeft: { lat: 48.8584, lng: 2.2945 },
        bottomRight: { lat: 48.8554, lng: 2.2975 },
      },
      keyHash: "a1b2c3d4e5f6g7h8i9j0",
    },
    {
      id: "ch2",
      name: "Decode the Signal",
      shortName: "Signal",
      points: 150,
      location: {
        topLeft: { lat: 48.8614, lng: 2.3375 },
        bottomRight: { lat: 48.8584, lng: 2.3405 },
      },
      keyHash: "b2c3d4e5f6g7h8i9j0k1",
    },
    {
      id: "ch3",
      name: "Capture the Flag",
      shortName: "CTF",
      points: 200,
      location: {
        topLeft: { lat: 48.8744, lng: 2.2945 },
        bottomRight: { lat: 48.8714, lng: 2.2975 },
      },
      keyHash: "c3d4e5f6g7h8i9j0k1l2",
    },
  ])
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [serialPorts, setSerialPorts] = useState<SerialPort[]>([])
  const [isLoadingPorts, setIsLoadingPorts] = useState(false)
  const [selectedPort, setSelectedPort] = useState<string>("")
  const [isReadingTag, setIsReadingTag] = useState(false)

  // New challenge form state
  const [newChallenge, setNewChallenge] = useState<Omit<Challenge, "id">>({
    name: "",
    shortName: "",
    points: 100,
    location: {
      topLeft: { lat: 0, lng: 0 },
      bottomRight: { lat: 0, lng: 0 },
    },
    keyHash: "",
  })

  useEffect(() => {
    // Load serial ports when dialog opens
    if (isDialogOpen) {
      fetchSerialPorts()
    }
  }, [isDialogOpen])

  const fetchSerialPorts = async () => {
    setIsLoadingPorts(true)
    try {
      // In a real app, this would be an API call
      // For now, we'll use mock data
      await new Promise((resolve) => setTimeout(resolve, 500))

      // Mock data - in a real app, this would come from the API
      const mockPorts = [
        { path: "COM1", manufacturer: "STMicroelectronics", serialNumber: "STM32-001" },
        { path: "COM3", manufacturer: "FTDI", serialNumber: "FT232R-002" },
        { path: "/dev/ttyUSB0", manufacturer: "Silicon Labs", serialNumber: "CP2102-003" },
        { path: "/dev/ttyACM0", manufacturer: "STMicroelectronics", serialNumber: "STM32-004" },
      ]

      setSerialPorts(mockPorts)
    } catch (error) {
      console.error("Failed to fetch serial ports:", error)
      toast("Error", {
        description: "Failed to load available serial ports. Please try again.",
      })
    } finally {
      setIsLoadingPorts(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target

    if (name.includes(".")) {
      const [parent, child, prop] = name.split(".")
      setNewChallenge((prev) => ({
        ...prev,
        [parent]: {
          ...prev[parent as keyof typeof prev],
          [child]: {
            ...(prev[parent as keyof typeof prev] as any)[child],
            [prop]: Number.parseFloat(value),
          },
        },
      }))
    } else {
      setNewChallenge((prev) => ({
        ...prev,
        [name]: name === "points" ? Number.parseInt(value) : value,
      }))
    }
  }

  const handleReadNfcTag = async () => {
    if (!selectedPort) {
      toast("Error", {
        description: "Please select a serial port first.",
      })
      return
    }

    setIsReadingTag(true)
    try {
      // In a real app, this would be an API call
      // For now, we'll simulate it
      await new Promise((resolve) => setTimeout(resolve, 2000))

      // Mock response - in a real app, this would come from the API
      const mockHash =
        "$2b$10$" + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)

      setNewChallenge((prev) => ({
        ...prev,
        keyHash: mockHash,
      }))

      toast("Success", {
        description: "NFC tag read successfully!",
      })
    } catch (error) {
      console.error("Failed to read NFC tag:", error)
      toast("Error", {
        description: "Failed to read NFC tag. Please try again.",
      })
    } finally {
      setIsReadingTag(false)
    }
  }

  const handleCreateChallenge = () => {
    // Validate form
    if (!newChallenge.name.trim() || !newChallenge.shortName.trim()) {
      toast("Error", {
        description: "Challenge name and short name are required",
      })
      return
    }

    if (!newChallenge.keyHash) {
      toast("Error", {
        description: "Please read an NFC tag to generate a key hash",
      })
      return
    }

    // In a real app, this would call an API to create the challenge
    const newChallengeWithId: Challenge = {
      ...newChallenge,
      id: `ch${challenges.length + 1}`,
    }

    setChallenges([...challenges, newChallengeWithId])
    setIsDialogOpen(false)

    // Reset form
    setNewChallenge({
      name: "",
      shortName: "",
      points: 100,
      location: {
        topLeft: { lat: 0, lng: 0 },
        bottomRight: { lat: 0, lng: 0 },
      },
      keyHash: "",
    })
    setSelectedPort("")

    toast("Challenge created", {
      description: `Challenge "${newChallenge.name}" has been created successfully.`,
    })
  }

  const handleDeleteChallenge = (id: string, name: string) => {
    setChallenges(challenges.filter((challenge) => challenge.id !== id))
    toast("Challenge deleted", {
      description: `Challenge "${name}" has been deleted successfully.`,
    })
  }

  const formatCoordinates = (lat: number, lng: number) => {
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Challenges</CardTitle>
            <CardDescription>Create and manage challenges for the competition</CardDescription>
          </div>
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Add Challenge
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Create New Challenge</DialogTitle>
                <DialogDescription>
                  Add a new challenge to the competition. Define its name, location, and other properties.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid gap-2">
                  <Label htmlFor="name">Challenge Name</Label>
                  <Input
                    id="name"
                    name="name"
                    value={newChallenge.name}
                    onChange={handleInputChange}
                    placeholder="Enter challenge name"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="shortName">Short Name</Label>
                  <Input
                    id="shortName"
                    name="shortName"
                    value={newChallenge.shortName}
                    onChange={handleInputChange}
                    placeholder="Enter short name"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="points">Points</Label>
                  <Input
                    id="points"
                    name="points"
                    type="number"
                    value={newChallenge.points}
                    onChange={handleInputChange}
                    min="1"
                  />
                </div>
                <div className="grid gap-2">
                  <Label>Location (Map Area)</Label>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="topLeftLat" className="text-xs">
                        Top Left Latitude
                      </Label>
                      <Input
                        id="topLeftLat"
                        name="location.topLeft.lat"
                        type="number"
                        step="0.0001"
                        value={newChallenge.location.topLeft.lat}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div>
                      <Label htmlFor="topLeftLng" className="text-xs">
                        Top Left Longitude
                      </Label>
                      <Input
                        id="topLeftLng"
                        name="location.topLeft.lng"
                        type="number"
                        step="0.0001"
                        value={newChallenge.location.topLeft.lng}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div>
                      <Label htmlFor="bottomRightLat" className="text-xs">
                        Bottom Right Latitude
                      </Label>
                      <Input
                        id="bottomRightLat"
                        name="location.bottomRight.lat"
                        type="number"
                        step="0.0001"
                        value={newChallenge.location.bottomRight.lat}
                        onChange={handleInputChange}
                      />
                    </div>
                    <div>
                      <Label htmlFor="bottomRightLng" className="text-xs">
                        Bottom Right Longitude
                      </Label>
                      <Input
                        id="bottomRightLng"
                        name="location.bottomRight.lng"
                        type="number"
                        step="0.0001"
                        value={newChallenge.location.bottomRight.lng}
                        onChange={handleInputChange}
                      />
                    </div>
                  </div>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="serialPort">Select port where NFC reader is connected</Label>
                  <div className="flex gap-2">
                    <Select value={selectedPort} onValueChange={setSelectedPort}>
                      <SelectTrigger className="flex-1">
                        <SelectValue placeholder="Select port" />
                      </SelectTrigger>
                      <SelectContent>
                        {serialPorts.map((port) => (
                          <SelectItem key={port.path} value={port.path}>
                            {port.path} - {port.manufacturer}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button variant="outline" size="icon" onClick={fetchSerialPorts} disabled={isLoadingPorts}>
                      <RefreshCw className={`h-4 w-4 ${isLoadingPorts ? "animate-spin" : ""}`} />
                    </Button>
                  </div>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="keyHash">Key Hash</Label>
                  <div className="flex gap-2">
                    <Input
                      id="keyHash"
                      name="keyHash"
                      value={newChallenge.keyHash}
                      readOnly
                      placeholder="Read NFC tag to generate hash"
                      className="flex-1 font-mono text-xs"
                    />
                    <Button onClick={handleReadNfcTag} disabled={!selectedPort || isReadingTag}>
                      {isReadingTag ? (
                        <>
                          <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                          Reading...
                        </>
                      ) : (
                        "Read Tag"
                      )}
                    </Button>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    This hash will be used to verify the NFC tag during the challenge.
                  </p>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateChallenge}>Create Challenge</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Short Name</TableHead>
                  <TableHead>Points</TableHead>
                  <TableHead>Location</TableHead>
                  <TableHead>Key Hash</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {challenges.map((challenge) => (
                  <TableRow key={challenge.id}>
                    <TableCell className="font-medium">{challenge.name}</TableCell>
                    <TableCell>{challenge.shortName}</TableCell>
                    <TableCell>{challenge.points}</TableCell>
                    <TableCell>
                      <div className="flex flex-col text-xs">
                        <span>
                          TL: {formatCoordinates(challenge.location.topLeft.lat, challenge.location.topLeft.lng)}
                        </span>
                        <span>
                          BR:{" "}
                          {formatCoordinates(challenge.location.bottomRight.lat, challenge.location.bottomRight.lng)}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-xs">{challenge.keyHash.substring(0, 10)}...</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="outline" size="icon" asChild>
                          <Link href={`/challenges/${challenge.id}`}>
                            <Edit className="h-4 w-4" />
                            <span className="sr-only">Edit Challenge</span>
                          </Link>
                        </Button>
                        <Button
                          variant="outline"
                          size="icon"
                          onClick={() => handleDeleteChallenge(challenge.id, challenge.name)}
                        >
                          <Trash className="h-4 w-4" />
                          <span className="sr-only">Delete Challenge</span>
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
