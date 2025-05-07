import app from "./src/app.js";
import { initializeDatabase } from "./src/db.js";

const PORT = process.env.IOTVENTURE_BACKEND_PORT || 3000;

function startServer() {
  try {
    initializeDatabase()
    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`)
    })
  } catch (error) {
    console.error("Failed to start server:", error)
  }
}

startServer()
