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
import { type Challenge, /*type SerialPort, getSerialPorts, readNfcTag,*/ getChallenge, updateChallenge } from "@/lib/api"
import { randomBytes } from "crypto"

interface SerialPort {
  path: string,
  manufacturer: string
}

export default function ChallengeDetailsPage() {
  const params = useParams()
  const router = useRouter()
  const challengeId = params.id as string
  const mapContainerRef = useRef<HTMLDivElement>(null)

  const [challenge, setChallenge] = useState<Challenge | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [serialOutput, setSerialOutput] = useState<string[]>([])
  const [serialPorts, setSerialPorts] = useState<SerialPort[]>([])
  const [selectedPort, setSelectedPort] = useState<string>("")
  const [isLoadingPorts, setIsLoadingPorts] = useState(false)
  const [isReadingTag, setIsReadingTag] = useState(false)
  const [mapInitialized, setMapInitialized] = useState(false)
  const serialOutputRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // Load challenge data and serial ports when component mounts
    fetchChallengeData()
    fetchSerialPorts()
  }, [challengeId])

  useEffect(() => {
    if (challenge && mapContainerRef.current && !mapInitialized) {
      initializeMap()
    }
  }, [challenge, mapContainerRef.current])

  useEffect(() => {
    if (serialOutputRef.current) {
      const el = serialOutputRef.current
      el.scrollTop = el.scrollHeight
    }
  }, [serialOutput])

  // Update the fetchChallengeData function to use the real API
  const fetchChallengeData = async () => {
    setLoading(true)
    try {
      // Call the API to get the challenge
      const data = await getChallenge(challengeId)
      if ( data.status !== 200 ) {
        setChallenge(null)
        router.replace("/challenges")
        return
      }
      setChallenge(data?.challenge || null)
    } catch (error) {
      console.error("Failed to fetch challenge data:", error)
      toast("Error", {
        description: error instanceof Error ? error.message : "Failed to load challenge data. Please try again.",
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchSerialPorts = async () => {
    setIsLoadingPorts(true)
    try {
      const ports: SerialPort[] = [
        { path: "/dev/ttyACM0", manufacturer: "" },
        { path: "/dev/ttyUSB0", manufacturer: "" },
      ]; //await getSerialPorts()
      setSerialPorts(ports)
    } catch (error) {
      console.error("Failed to fetch serial ports:", error)
      toast("Error", {
        description:
          error instanceof Error ? error.message : "Failed to load available serial ports. Please try again.",
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
    if (!challenge) return;
  
    const { name, value } = e.target;
  
    if (name.includes(".")) {
      const [parent, child, prop] = name.split(".");
  
      setChallenge((prev) => {
        if (!prev) return prev;
  
        const parentVal = prev[parent as keyof Challenge];
        if (typeof parentVal !== "object" || parentVal === null) return prev;
  
        const childVal = (parentVal as any)[child];
        if (typeof childVal !== "object" || childVal === null) return prev;
    
        return {
          ...prev,
          [parent]: {
            ...parentVal,
            [child]: {
              ...childVal,
              [prop]: Number.parseFloat(value) || 0.0,
            },
          },
        };
      });
    } else {
      setChallenge((prev) => {
        if (!prev) return prev;
  
        if (name === "points") {
          const parsed = Number.parseInt(value) || 0;
          return { ...prev, points: parsed };
        }
  
        return { ...prev, [name]: value };
      });
    }
  };
  
  // Update the handleSaveChallenge function to use the real API
  const handleSaveChallenge = async () => {
    if (!challenge) return

    setSaving(true)
    try {
      const updatedChallenge = await updateChallenge(challenge.id, challenge)

      setChallenge(updatedChallenge?.challenge || null)

      toast("Challenge updated",{
        description: `Challenge "${challenge.name}" has been updated successfully.`,
      })
    } catch (error) {
      console.error("Failed to update challenge:", error)
      toast("Error", {
        description: error instanceof Error ? error.message : "Failed to update challenge. Please try again.",
      })
    } finally {
      setSaving(false)
    }
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
      setSerialOutput((prev) => [...prev, `Initializing serial connection on ${selectedPort}...`])
      setSerialOutput((prev) => [...prev, "Searching for NFC device..."])
      setSerialOutput((prev) => [...prev, `Device found on ${selectedPort}`])

      setSerialOutput((prev) => [...prev, "Sending CONNECT_SERVER command..."])

      await new Promise((resolve) => setTimeout(resolve, 100))

      setSerialOutput((prev) => [...prev, "Waiting for response..."])
      setSerialOutput((prev) => [...prev, "Received: CONNECT_OK"])

      setSerialOutput((prev) => [...prev, "Sending READ_TAG command..."])

      await new Promise((resolve) => setTimeout(resolve, 100))

      setSerialOutput((prev) => [...prev, "Waiting for tag..."])
      setSerialOutput((prev) => [...prev, "Received: TAG_READ"])

      setSerialOutput((prev) => [...prev, "Sending READ_ID command..."])

      // Simulate a delay for the API call
      await new Promise((resolve) => setTimeout(resolve, 1000))

      // Make the actual API call
      const keyID = randomBytes(7).toString("hex") // await readNfcTag(selectedPort)

      setSerialOutput((prev) => [...prev, "Received tag ID: " + keyID])
      setSerialOutput((prev) => [...prev, "Generating hash..."])
      setSerialOutput((prev) => [...prev, "Hash generated successfully!"])

      setChallenge({
        ...challenge,
        keyHash: keyID, // TODO: Hash the ID
      })

      toast("Success", {
        description: "NFC tag read successfully!",
      })
    } catch (error) {
      console.error("Failed to read NFC tag:", error)
      setSerialOutput((prev) => [...prev, "Error: Failed to read NFC tag"])

      toast("Error", {
        description: error instanceof Error ? error.message : "Failed to read NFC tag. Please try again.",
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
                  <Input id="keyHash" name="keyHash" value={challenge.keyHash} readOnly />
                  <p className="text-xs text-muted-foreground">
                    This hash is generated when connecting to the NFC reader. Use the NFC Reader tab to update it.
                  </p>
                </div>
                {/* <Button onClick={handleSaveChallenge} disabled={saving}>
                  {saving ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="mr-2 h-4 w-4" />
                      Save Changes
                    </>
                  )}
                </Button> */}
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
                  <div className="h-60 overflow-y-auto" ref={serialOutputRef}
                  >
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

                {/* <Button onClick={handleSaveChallenge} disabled={saving}>
                  {saving ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="mr-2 h-4 w-4" />
                      Save Location
                    </>
                  )}
                </Button> */}
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
        <CardFooter className="flex justify-between">
          <Button variant="outline" onClick={() => router.push("/challenges")}>
            Back to Challenges
          </Button>
          <Button onClick={handleSaveChallenge} disabled={saving}>
            {saving ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                Save All Changes
              </>
            )}
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}
