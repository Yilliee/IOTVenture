import type React from "react"
import { ThemeProvider } from "@/components/theme-provider"
import { Toaster } from "sonner"
import "./globals.css"
import { Inter } from "next/font/google"
import Header from "@/components/header"

const inter = Inter({ subsets: ["latin"] })

export const metadata = {
  title: "IoT Venture Portal",
  description: "Admin portal for IoT Venture game challenges",
    generator: 'v0.dev'
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem disableTransitionOnChange>
          <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex-1">{children}</main>
          </div>
          <Toaster />
        </ThemeProvider>
      </body>
    </html>
  )
}
