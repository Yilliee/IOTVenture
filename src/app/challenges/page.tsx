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
import { Plus, Trash, Edit, RefreshCw } from "lucide-react"
import { toast } from "sonner"
import Link from "next/link"
import {
  type Challenge,
  getChallenges,
  createChallenge,
  deleteChallenge,
} from "@/lib/api"


export default function ChallengesPage() {
  const [challenges, setChallenges] = useState<Challenge[]>([])
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isLoadingChallenges, setIsLoadingChallenges] = useState(true)

  const [newChallenge, setNewChallenge] = useState<Omit<Challenge, "id">>({
    name: "",
    shortName: "",
    points: 0,
    location: {
      topLeft: { lat: 0, lng: 0 },
      bottomRight: { lat: 0, lng: 0 },
    },
    keyHash: "",
  })

  useEffect(() => {
    fetchChallenges()
  }, [])

  const fetchChallenges = async () => {
    setIsLoadingChallenges(true)
    try {
      const response = await getChallenges()

      if (response.status === 200 && response.challenges) {
        setChallenges(response.challenges)
      } else {
        toast("Error", {
          description: response.error || "Failed to load challenges",
        })
      }
    } catch (error) {
      console.error("Failed to fetch challenges:", error)
      toast("Error", {
        description: "An unexpected error occurred while loading challenges",
      })
    } finally {
      setIsLoadingChallenges(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
  
    if (name.includes(".")) {
      const [parent, child, prop] = name.split(".");
  
      setNewChallenge((prev) => {
        const parentVal = prev[parent as keyof typeof prev];
        if (typeof parentVal !== "object" || parentVal === null) return prev;
  
        const childVal = (parentVal as any)[child];
        if (typeof childVal !== "object" || childVal === null) return prev;
  
        return {
          ...prev,
          [parent]: {
            ...parentVal,
            [child]: {
              ...childVal,
              [prop]: (Number.parseFloat(value) || ""),
            },
          },
        };
      });
    } else {
      setNewChallenge((prev) => ({
        ...prev,
        [name]: name === "points" ? (Number.parseInt(value) || "") : value,
      }));
    }
  };
  
  const handleCreateChallenge = async () => {
    if (!newChallenge.name.trim() || !newChallenge.shortName.trim()) {
      toast("Error", {
        description: "Challenge name and short name are required",
      })
      if (!newChallenge.keyHash) {
        toast("Error", {
          description: "Please read an NFC tag to generate a key hash",
        })
      }
      return
    }

    try {
      const response = await createChallenge(newChallenge)

      if (response.status === 201 && response.challenge) {
        setChallenges([...challenges, response.challenge])
        setIsDialogOpen(false)

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

        toast("Challenge created", {
          description: `Challenge "${newChallenge.name}" has been created successfully.`,
        })
      } else {
        toast("Error", {
          description: response.error || "Failed to create challenge",
        })
      }
    } catch (error) {
      console.error("Failed to create challenge:", error)
      toast("Error", {
        description: "An unexpected error occurred while creating the challenge",
      })
    }
  }

  const handleDeleteChallenge = async (id: string, name: string) => {
    try {
      const response = await deleteChallenge(id)

      if (response.status === 204 || response.status === 200) {
        setChallenges(challenges.filter((challenge) => challenge.id !== id))

        toast("Challenge deleted", {
          description: `Challenge "${name}" has been deleted successfully.`,
        })
      } else {
        toast("Error", {
          description: response.error || "Failed to delete challenge",
        })
      }
    } catch (error) {
      console.error("Failed to delete challenge:", error)
      toast("Error", {
        description: "An unexpected error occurred while deleting the challenge",
      })
    }
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
                  <Label htmlFor="keyHash">Key Hash</Label>
                  <Input
                    id="keyHash"
                    name="keyHash"
                    value={newChallenge.keyHash}
                    onChange={handleInputChange}
                    placeholder="Enter key hash"
                  />
                  <p className="text-xs text-muted-foreground">
                    This will be automatically generated when connecting to the STM32 device.
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
          {isLoadingChallenges ? (
              <div className="flex justify-center items-center py-8">
                <RefreshCw className="h-8 w-8 animate-spin text-primary" />
                <span className="ml-2">Loading challenges...</span>
              </div>
            ) : (
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
            )}
        </CardContent>
      </Card>
    </div>
  )
}
