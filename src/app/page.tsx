import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import Link from "next/link"
import { Trophy, Users, MapPin, MessageSquare, Settings } from "lucide-react"

export default function Home() {
  return (
    <div className="container mx-auto py-8">
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <Link href="/leaderboard">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <Trophy className="h-8 w-8 text-orange-500 mb-2" />
              <CardTitle>Leaderboard</CardTitle>
              <CardDescription>View the current standings of all teams</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Track team progress and challenge completions in real-time.</p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/teams">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <Users className="h-8 w-8 text-blue-500 mb-2" />
              <CardTitle>Team Management</CardTitle>
              <CardDescription>Create and manage teams</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Add teams, reset passwords, and manage team members.</p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/challenges">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <MapPin className="h-8 w-8 text-green-500 mb-2" />
              <CardTitle>Challenges</CardTitle>
              <CardDescription>Create and manage challenges</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Set up new challenges with locations and STM32 integration.</p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/messages">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <MessageSquare className="h-8 w-8 text-purple-500 mb-2" />
              <CardTitle>Messages</CardTitle>
              <CardDescription>Broadcast messages to teams</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Send announcements and instructions to specific teams.</p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/settings">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <Settings className="h-8 w-8 text-gray-500 mb-2" />
              <CardTitle>Settings</CardTitle>
              <CardDescription>Configure game settings</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Adjust game parameters and system configuration.</p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/final-submission">
          <Card className="h-full transition-all hover:shadow-md">
            <CardHeader>
              <div className="rounded-full bg-red-100 p-2 w-fit">
                <Trophy className="h-6 w-6 text-red-500" />
              </div>
              <CardTitle>Final Submission</CardTitle>
              <CardDescription>End of contest submissions</CardDescription>
            </CardHeader>
            <CardContent>
              <p>Track final team submissions and generate the official results.</p>
            </CardContent>
          </Card>
        </Link>
      </div>
    </div>
  )
}
