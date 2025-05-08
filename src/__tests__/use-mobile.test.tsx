import { renderHook, act } from "@testing-library/react"
import { useIsMobile } from "./use-mobile"

function setScreenWidth(width: number) {
  Object.defineProperty(window, "innerWidth", {
    writable: true,
    configurable: true,
    value: width,
  })
  window.dispatchEvent(new Event("resize"))
}

describe("useIsMobile", () => {
  beforeEach(() => {
    // Mock matchMedia
    window.matchMedia = jest.fn().mockImplementation(query => {
      return {
        matches: window.innerWidth < 768,
        media: query,
        onchange: null,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      }
    })
  })

  it("returns true when width is less than 768", () => {
    setScreenWidth(500)
    const { result } = renderHook(() => useIsMobile())
    expect(result.current).toBe(true)
  })

  it("returns false when width is 768 or more", () => {
    setScreenWidth(1024)
    const { result } = renderHook(() => useIsMobile())
    expect(result.current).toBe(false)
  })
})

