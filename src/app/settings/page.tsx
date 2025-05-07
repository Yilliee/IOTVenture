"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { toast } from "sonner"
import { Save, RefreshCw, SettingsIcon } from "lucide-react"

export default function SettingsPage() {
  const [generalSettings, setGeneralSettings] = useState({
    contestName: "IoT Venture Challenge 2023",
    contestStartTime: "2023-05-15T10:00",
    contestEndTime: "2023-05-15T16:00",
    allowRegistration: true,
    defaultMaxTeamMembers: 8,
  })

  const [serverSettings, setServerSettings] = useState({
    serverPort: "3000",
    databasePath: "./data/iotventure.db",
    logLevel: "info",
    enableDebugMode: false,
    autoBackup: true,
  })

  const [isSaving, setIsSaving] = useState(false)

  const handleGeneralSettingsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target
    setGeneralSettings({
      ...generalSettings,
      [name]: type === "checkbox" ? checked : value,
    })
  }

  const handleServerSettingsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target
    setServerSettings({
      ...serverSettings,
      [name]: type === "checkbox" ? checked : value,
    })
  }

  const handleSaveSettings = (settingsType: string) => {
    setIsSaving(true)

    // Simulate saving settings
    setTimeout(() => {
      setIsSaving(false)

      toast("Settings saved", {
        description: `${settingsType} settings have been updated successfully.`,
      })
    }, 1000)
  }

  const handleResetDatabase = () => {
    // In a real app, this would call an API to reset the database
    toast("Database reset", {
      description: "The database has been reset to its initial state.",
    })
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <SettingsIcon className="h-6 w-6 text-primary" />
            <CardTitle>Settings</CardTitle>
          </div>
          <CardDescription>Configure system settings for the IoT Venture Portal</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="general">
            <TabsList className="mb-4">
              <TabsTrigger value="general">General</TabsTrigger>
              <TabsTrigger value="server">Server</TabsTrigger>
              <TabsTrigger value="database">Database</TabsTrigger>
            </TabsList>

            <TabsContent value="general">
              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="contestName">Contest Name</Label>
                  <Input
                    id="contestName"
                    name="contestName"
                    value={generalSettings.contestName}
                    onChange={handleGeneralSettingsChange}
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="grid gap-2">
                    <Label htmlFor="contestStartTime">Contest Start Time</Label>
                    <Input
                      id="contestStartTime"
                      name="contestStartTime"
                      type="datetime-local"
                      value={generalSettings.contestStartTime}
                      onChange={handleGeneralSettingsChange}
                    />
                  </div>

                  <div className="grid gap-2">
                    <Label htmlFor="contestEndTime">Contest End Time</Label>
                    <Input
                      id="contestEndTime"
                      name="contestEndTime"
                      type="datetime-local"
                      value={generalSettings.contestEndTime}
                      onChange={handleGeneralSettingsChange}
                    />
                  </div>
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="defaultMaxTeamMembers">Default Max Team Members</Label>
                  <Input
                    id="defaultMaxTeamMembers"
                    name="defaultMaxTeamMembers"
                    type="number"
                    value={generalSettings.defaultMaxTeamMembers}
                    onChange={handleGeneralSettingsChange}
                    min="1"
                  />
                  <p className="text-sm text-muted-foreground">
                    This is the default maximum number of members allowed per team. This can be overridden for
                    individual teams.
                  </p>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="allowRegistration"
                    name="allowRegistration"
                    checked={generalSettings.allowRegistration}
                    onCheckedChange={(checked) =>
                      setGeneralSettings({ ...generalSettings, allowRegistration: checked })
                    }
                  />
                  <Label htmlFor="allowRegistration">Allow Team Registration</Label>
                </div>

                <Button onClick={() => handleSaveSettings("General")} disabled={isSaving}>
                  {isSaving ? (
                    <>
                      <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="mr-2 h-4 w-4" />
                      Save Settings
                    </>
                  )}
                </Button>
              </div>
            </TabsContent>

            <TabsContent value="server">
              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="serverPort">Server Port</Label>
                  <Input
                    id="serverPort"
                    name="serverPort"
                    value={serverSettings.serverPort}
                    onChange={handleServerSettingsChange}
                  />
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="databasePath">Database Path</Label>
                  <Input
                    id="databasePath"
                    name="databasePath"
                    value={serverSettings.databasePath}
                    onChange={handleServerSettingsChange}
                  />
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="logLevel">Log Level</Label>
                  <select
                    id="logLevel"
                    name="logLevel"
                    value={serverSettings.logLevel}
                    onChange={(e) => setServerSettings({ ...serverSettings, logLevel: e.target.value })}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <option value="error">Error</option>
                    <option value="warn">Warning</option>
                    <option value="info">Info</option>
                    <option value="debug">Debug</option>
                  </select>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="enableDebugMode"
                    name="enableDebugMode"
                    checked={serverSettings.enableDebugMode}
                    onCheckedChange={(checked) => setServerSettings({ ...serverSettings, enableDebugMode: checked })}
                  />
                  <Label htmlFor="enableDebugMode">Enable Debug Mode</Label>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="autoBackup"
                    name="autoBackup"
                    checked={serverSettings.autoBackup}
                    onCheckedChange={(checked) => setServerSettings({ ...serverSettings, autoBackup: checked })}
                  />
                  <Label htmlFor="autoBackup">Automatic Database Backup</Label>
                </div>

                <Button onClick={() => handleSaveSettings("Server")} disabled={isSaving}>
                  {isSaving ? (
                    <>
                      <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="mr-2 h-4 w-4" />
                      Save Settings
                    </>
                  )}
                </Button>
              </div>
            </TabsContent>

            <TabsContent value="database">
              <div className="grid gap-6">
                <div>
                  <h3 className="text-lg font-medium">Database Management</h3>
                  <p className="text-sm text-muted-foreground mt-1">
                    Manage the SQLite database for the IoT Venture Portal.
                  </p>
                </div>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">Database Information</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <p className="font-medium">Database Type</p>
                        <p className="text-muted-foreground">SQLite</p>
                      </div>
                      <div>
                        <p className="font-medium">Database Size</p>
                        <p className="text-muted-foreground">2.4 MB</p>
                      </div>
                      <div>
                        <p className="font-medium">Last Backup</p>
                        <p className="text-muted-foreground">2023-05-15 09:00</p>
                      </div>
                      <div>
                        <p className="font-medium">Tables</p>
                        <p className="text-muted-foreground">8</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <div className="grid gap-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Button variant="outline">Backup Database</Button>
                    <Button variant="outline">Restore from Backup</Button>
                  </div>

                  <div className="border-t pt-4">
                    <h4 className="text-sm font-medium mb-2">Danger Zone</h4>
                    <div className="grid gap-2">
                      <p className="text-sm text-muted-foreground">
                        These actions are destructive and cannot be undone. Please be careful.
                      </p>
                      <Button variant="destructive" onClick={handleResetDatabase}>
                        Reset Database
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  )
}
