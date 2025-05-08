// __tests__/ModeToggle.test.tsx
import { render, screen, fireEvent } from "@testing-library/react"
import { ModeToggle } from "../components/mode-toggle"
import "@testing-library/jest-dom"
import { useTheme } from "next-themes"

// Mock useTheme
jest.mock("next-themes", () => ({
  useTheme: jest.fn(),
}))

describe("ModeToggle component", () => {
  const setThemeMock = jest.fn()

  beforeEach(() => {
    ;(useTheme as jest.Mock).mockReturnValue({
      setTheme: setThemeMock,
    })
    setThemeMock.mockClear()
  })

  it("renders the toggle button", () => {
    render(<ModeToggle />)
    expect(screen.getByRole("button", { name: /toggle theme/i })).toBeInTheDocument()
  })

  it("shows theme options on click", () => {
    render(<ModeToggle />)

    const toggleButton = screen.getByRole("button", { name: /toggle theme/i })
    fireEvent.click(toggleButton)

    expect(screen.getByText("Light")).toBeInTheDocument()
    expect(screen.getByText("Dark")).toBeInTheDocument()
    expect(screen.getByText("System")).toBeInTheDocument()
  })

  it("calls setTheme with 'light' when Light is clicked", () => {
    render(<ModeToggle />)
    fireEvent.click(screen.getByRole("button", { name: /toggle theme/i }))
    fireEvent.click(screen.getByText("Light"))
    expect(setThemeMock).toHaveBeenCalledWith("light")
  })

  it("calls setTheme with 'dark' when Dark is clicked", () => {
    render(<ModeToggle />)
    fireEvent.click(screen.getByRole("button", { name: /toggle theme/i }))
    fireEvent.click(screen.getByText("Dark"))
    expect(setThemeMock).toHaveBeenCalledWith("dark")
  })

  it("calls setTheme with 'system' when System is clicked", () => {
    render(<ModeToggle />)
    fireEvent.click(screen.getByRole("button", { name: /toggle theme/i }))
    fireEvent.click(screen.getByText("System"))
    expect(setThemeMock).toHaveBeenCalledWith("system")
  })
})

