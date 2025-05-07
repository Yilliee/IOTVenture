"use client"

import type React from "react"

import { useState, useEffect, useRef } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { toast } from "sonner"
import { MapPin, Save, RefreshCw, Terminal, Loader2 } from "lucide-react"

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

export default function ChallengeDetailsPage() {
  const params = useParams()
  const router = useRouter()
  const challengeId = params.id as string
  const mapContainerRef = useRef<HTMLDivElement>(null)

  const [challenge, setChallenge] = useState<Challenge | null>(null)
  const [loading, setLoading] = useState(true)
  const [connecting, setConnecting] = useState(false)
  const [serialOutput, setSerialOutput] = useState<string[]>([])
  const [serialPorts, setSerialPorts] = useState<SerialPort[]>([])
  const [selectedPort, setSelectedPort] = useState<string>("")
  const [isLoadingPorts, setIsLoadingPorts] = useState(false)
  const [isReadingTag, setIsReadingTag] = useState(false)
  const [mapInitialized, setMapInitialized] = useState(false)

  useEffect(() => {
    // In a real app, this would be an API call
    fetchChallengeData()
    fetchSerialPorts()
  }, [challengeId])

  useEffect(() => {
    if (challenge && mapContainerRef.current && !mapInitialized) {
      initializeMap()
    }
  }, [challenge, mapContainerRef.current])

  const fetchChallengeData = async () => {
    setLoading(true)
    try {
      // Mock data - would be replaced with a real fetch in production
      await new Promise((resolve) => setTimeout(resolve, 1000))

      // Different mock data based on challenge ID
      if (challengeId === "ch1") {
        setChallenge({
          id: "ch1",
          name: "Find the Beacon",
          shortName: "Beacon",
          points: 100,
          location: {
            topLeft: { lat: 48.8584, lng: 2.2945 },
            bottomRight: { lat: 48.8554, lng: 2.2975 },
          },
          keyHash: "a1b2c3d4e5f6g7h8i9j0",
        })
      } else if (challengeId === "ch2") {
        setChallenge({
          id: "ch2",
          name: "Decode the Signal",
          shortName: "Signal",
          points: 150,
          location: {
            topLeft: { lat: 48.8614, lng: 2.3375 },
            bottomRight: { lat: 48.8584, lng: 2.3405 },
          },
          keyHash: "b2c3d4e5f6g7h8i9j0k1",
        })
      } else {
        setChallenge({
          id: "ch3",
          name: "Capture the Flag",
          shortName: "CTF",
          points: 200,
          location: {
            topLeft: { lat: 48.8744, lng: 2.2945 },
            bottomRight: { lat: 48.8714, lng: 2.2975 },
          },
          keyHash: "c3d4e5f6g7h8i9j0k1l2",
        })
      }
    } catch (error) {
      console.error("Failed to fetch challenge data:", error)
      toast("Error", {
        description: "Failed to load challenge data. Please try again.",
      })
    } finally {
      setLoading(false)
    }
  }

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

  const initializeMap = () => {
    if (!challenge || !mapContainerRef.current) return

    // In a real app, this would initialize an OpenMaps/Leaflet map
    // For this demo, we'll just simulate it
    console.log("Initializing map with coordinates:", challenge.location)
    setMapInitialized(true)

    // Mock map initialization
    const mapContainer = mapContainerRef.current
    mapContainer.innerHTML = `
      <div class="flex items-center justify-center h-full bg-gray-100 rounded-md">
        <div class="text-center p-4">
          <p class="font-medium">Map Preview</p>
          <p class="text-sm text-muted-foreground">Top Left: ${challenge.location.topLeft.lat.toFixed(4)}, ${challenge.location.topLeft.lng.toFixed(4)}</p>
          <p class="text-sm text-muted-foreground">Bottom Right: ${challenge.location.bottomRight.lat.toFixed(4)}, ${challenge.location.bottomRight.lng.toFixed(4)}</p>
        </div>
      </div>
    `
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!challenge) return

    const { name, value } = e.target

    if (name.includes(".")) {
      const [parent, child, prop] = name.split(".")
      setChallenge({
        ...challenge,
        [parent]: {
          ...challenge[parent as keyof typeof challenge],
          [child]: {
            ...(challenge[parent as keyof typeof challenge] as any)[child],
            [prop]: Number.parseFloat(value),
          },
        },
      })
    } else {
      setChallenge({
        ...challenge,
        [name]: name === "points" ? Number.parseInt(value) : value,
      })
    }
  }

  const handleSaveChallenge = () => {
    if (!challenge) return

    // In a real app, this would call an API to update the challenge
    toast("Challenge updated", {
      description: `Challenge "${challenge.name}" has been updated successfully.`,
    })
  }

  const handleReadNfcTag = async () => {
    if (!challenge || !selectedPort) {
      toast("Error", {
        description: "Please select a serial port first.",
      })
      return
    }

    setIsReadingTag(true)
    setSerialOutput([])

    try {
      // Simulate serial connection and output
      const outputs = [
        `Initializing serial connection on ${selectedPort}...`,
        "Searching for STM32 device...",
        `Device found on ${selectedPort}`,
        "Sending CONNECT_SERVER command...",
        "Waiting for response...",
        "Received: CONNECT_OK",
        "Sending READ_TAG command...",
        "Waiting for tag...",
        "Received: TAG_READ",
        "Sending READ_ID command...",
        "Received tag ID: 04A2B9C3D7E5F1",
        "Generating hash...",
        "Hash generated successfully!",
      ]

      for (const output of outputs) {
        await new Promise((resolve) => setTimeout(resolve, 400))
        setSerialOutput((prev) => [...prev, output])
      }

      // Mock hash generation
      const mockHash =
        "$2b$10$" + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)

      setChallenge({
        ...challenge,
        keyHash: mockHash,
      })

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

  if (loading) {
    return (
      <div className="container py-8">
        <Card>
          <CardContent className="pt-6">
            <div className="flex justify-center items-center h-40">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="ml-2">Loading challenge data...</p>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (!challenge) {
    return (
      <div className="container py-8">
        <Card>
          <CardContent className="pt-6">
            <div className="flex justify-center items-center h-40">
              <p>Challenge not found</p>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <MapPin className="h-6 w-6 text-primary" />
            <CardTitle>Edit Challenge: {challenge.name}</CardTitle>
          </div>
          <CardDescription>Challenge ID: {challengeId}</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="details">
            <TabsList className="mb-4">
              <TabsTrigger value="details">Challenge Details</TabsTrigger>
              <TabsTrigger value="device">NFC Reader</TabsTrigger>
              <TabsTrigger value="map">Map Location</TabsTrigger>
            </TabsList>

            <TabsContent value="details">
              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="name">Challenge Name</Label>
                  <Input id="name" name="name" value={challenge.name} onChange={handleInputChange} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="shortName">Short Name</Label>
                  <Input id="shortName" name="shortName" value={challenge.shortName} onChange={handleInputChange} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="points">Points</Label>
                  <Input
                    id="points"
                    name="points"
                    type="number"
                    value={challenge.points}
                    onChange={handleInputChange}
                    min="1"
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="keyHash">Key Hash</Label>
                  <Input id="keyHash" name="keyHash" value={challenge.keyHash} onChange={handleInputChange} readOnly />
                  <p className="text-xs text-muted-foreground">
                    This hash is generated when connecting to the NFC reader. Use the NFC Reader tab to update it.
                  </p>
                </div>
                <Button onClick={handleSaveChallenge}>
                  <Save className="mr-2 h-4 w-4" />
                  Save Changes
                </Button>
              </div>
            </TabsContent>

            <TabsContent value="device">
              <div className="grid gap-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-medium">NFC Reader Connection</h3>
                    <p className="text-sm text-muted-foreground">
                      Connect to an NFC reader to generate a unique key hash for this challenge.
                    </p>
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

                <div className="border rounded-md p-4 bg-black text-white font-mono text-sm">
                  <div className="h-60 overflow-y-auto">
                    {serialOutput.length === 0 ? (
                      <p className="text-gray-500">Select a port and click "Read NFC Tag" to start the process...</p>
                    ) : (
                      serialOutput.map((line, index) => (
                        <div key={index} className="mb-1">
                          <span className="text-green-500">{">"}</span> {line}
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="current-hash">Current Key Hash</Label>
                  <div className="flex gap-2">
                    <Input id="current-hash" value={challenge.keyHash} readOnly className="font-mono flex-1" />
                    <Button
                      onClick={handleReadNfcTag}
                      disabled={isReadingTag || !selectedPort}
                      className="min-w-[140px]"
                    >
                      {isReadingTag ? (
                        <>
                          <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                          Reading...
                        </>
                      ) : (
                        <>
                          <Terminal className="mr-2 h-4 w-4" />
                          Read NFC Tag
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="map">
              <div className="grid gap-4">
                <div>
                  <h3 className="text-lg font-medium">Challenge Location</h3>
                  <p className="text-sm text-muted-foreground">
                    Define the area on the map where this challenge will take place.
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="topLeftLat">Top Left Latitude</Label>
                    <Input
                      id="topLeftLat"
                      name="location.topLeft.lat"
                      type="number"
                      step="0.0001"
                      value={challenge.location.topLeft.lat}
                      onChange={handleInputChange}
                    />
                  </div>
                  <div>
                    <Label htmlFor="topLeftLng">Top Left Longitude</Label>
                    <Input
                      id="topLeftLng"
                      name="location.topLeft.lng"
                      type="number"
                      step="0.0001"
                      value={challenge.location.topLeft.lng}
                      onChange={handleInputChange}
                    />
                  </div>
                  <div>
                    <Label htmlFor="bottomRightLat">Bottom Right Latitude</Label>
                    <Input
                      id="bottomRightLat"
                      name="location.bottomRight.lat"
                      type="number"
                      step="0.0001"
                      value={challenge.location.bottomRight.lat}
                      onChange={handleInputChange}
                    />
                  </div>
                  <div>
                    <Label htmlFor="bottomRightLng">Bottom Right Longitude</Label>
                    <Input
                      id="bottomRightLng"
                      name="location.bottomRight.lng"
                      type="number"
                      step="0.0001"
                      value={challenge.location.bottomRight.lng}
                      onChange={handleInputChange}
                    />
                  </div>
                </div>

                <div className="border rounded-md h-80" ref={mapContainerRef}>
                  <div className="flex items-center justify-center h-full">
                    <p className="text-center text-muted-foreground">Loading map...</p>
                  </div>
                </div>

                <Button onClick={handleSaveChallenge}>
                  <Save className="mr-2 h-4 w-4" />
                  Save Location
                </Button>
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
        <CardFooter className="flex justify-between">
          <Button variant="outline" onClick={() => router.push("/challenges")}>
            Back to Challenges
          </Button>
          <Button onClick={handleSaveChallenge}>
            <Save className="mr-2 h-4 w-4" />
            Save All Changes
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}
