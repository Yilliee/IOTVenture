"use client"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { ModeToggle } from "@/components/mode-toggle"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Trophy, Users, MapPin, MessageSquare, Settings, LogOut, Menu } from "lucide-react"
import { useState } from "react"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"

export default function Header() {
  const pathname = usePathname()
  const [isLoggedIn, setIsLoggedIn] = useState(true) // This would be from your auth context in a real app

  const navigation = [
    { name: "Leaderboard", href: "/leaderboard", icon: Trophy },
    { name: "Teams", href: "/teams", icon: Users },
    { name: "Challenges", href: "/challenges", icon: MapPin },
    { name: "Messages", href: "/messages", icon: MessageSquare },
    { name: "Settings", href: "/settings", icon: Settings },
  ]

  const NavLinks = () => (
    <>
      {navigation.map((item) => {
        const isActive = pathname === item.href
        return (
          <Link
            key={item.name}
            href={item.href}
            className={`px-3 py-2 rounded-md text-sm font-medium ${
              isActive
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            }`}
          >
            {item.name}
          </Link>
        )
      })}
    </>
  )

  return (
    <header className="border-b">
      <div className="container mx-auto flex h-16 items-center justify-between px-4">
        <div className="flex items-center">
          <Link href="/" className="flex items-center">
            <Trophy className="h-6 w-6 mr-2 text-primary" />
            <span className="text-xl font-bold">IoT Venture</span>
          </Link>

          {/* Desktop navigation */}
          <nav className="ml-10 hidden md:flex items-center space-x-4">
            <NavLinks />
          </nav>
        </div>

        <div className="flex items-center gap-4">
          <ModeToggle />

          {isLoggedIn ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-8 w-8 rounded-full">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted">
                    <span className="text-sm font-medium">A</span>
                  </div>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>Admin Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem>Profile</DropdownMenuItem>
                <DropdownMenuItem>
                  <Link href="/final-submission" className="flex w-full">
                    Final Submission
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => setIsLoggedIn(false)}>
                  <LogOut className="mr-2 h-4 w-4" />
                  <span>Log out</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Button asChild>
              <Link href="/login">Login</Link>
            </Button>
          )}

          {/* Mobile menu */}
          <Sheet>
            <SheetTrigger asChild className="md:hidden">
              <Button variant="ghost" size="icon">
                <Menu className="h-6 w-6" />
                <span className="sr-only">Toggle menu</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="right">
              <div className="flex flex-col space-y-4 mt-8">
                <NavLinks />
                <Link
                  href="/final-submission"
                  className="px-3 py-2 rounded-md text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
                >
                  Final Submission
                </Link>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
