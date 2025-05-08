// __tests__/Header.test.tsx
import { render, screen, fireEvent } from "@testing-library/react"
import Header from "../components/header"
import "@testing-library/jest-dom"
import { usePathname } from "next/navigation"

// Mock next/navigation
jest.mock("next/navigation", () => ({
  usePathname: jest.fn(),
}))

// Mock next/link to just render <a>
jest.mock("next/link", () => {
  return ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  )
})

describe("Header component", () => {
  beforeEach(() => {
    // Reset mocks before each test
    (usePathname as jest.Mock).mockReturnValue("/")
  })

  it("renders site name", () => {
    render(<Header />)
    expect(screen.getByText("IoT Venture")).toBeInTheDocument()
  })

  it("renders all navigation links", () => {
    render(<Header />)
    expect(screen.getByText("Leaderboard")).toBeInTheDocument()
    expect(screen.getByText("Teams")).toBeInTheDocument()
    expect(screen.getByText("Challenges")).toBeInTheDocument()
    expect(screen.getByText("Messages")).toBeInTheDocument()
    expect(screen.getByText("Settings")).toBeInTheDocument()
  })

  it("highlights the active link", () => {
    ;(usePathname as jest.Mock).mockReturnValue("/teams")
    render(<Header />)
    const activeLink = screen.getByText("Teams")
    expect(activeLink).toHaveClass("bg-primary")
  })

  it("shows avatar and dropdown when logged in", () => {
    render(<Header />)
    expect(screen.getByText("A")).toBeInTheDocument()
  })

  it("opens mobile menu on button click", () => {
    render(<Header />)
    const button = screen.getByRole("button", { name: /toggle menu/i })
    fireEvent.click(button)
    // Ideally wait for sheet content — this may need additional setup depending on your Sheet implementation
    expect(screen.getByText("Final Submission")).toBeInTheDocument()
  })
})

