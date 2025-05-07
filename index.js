const express = require("express")
const cors = require("cors")
const Database = require("better-sqlite3")
const path = require("path")
const crypto = require("crypto")
const bodyParser = require("body-parser")
const cookieParser = require("cookie-parser")

// Initialize express app
const app = express()
const PORT = process.env.BACKEND_PORT || 3000


app.use(
  cors({
    origin: process.env.FRONTEND_URL || "http://localhost:3000",
    credentials: true,
  }),
)
app.use(bodyParser.json())
app.use(cookieParser())

// Database setup
let db

function initializeDatabase() {
  // Open database
  db = new Database(path.join(__dirname, "database.sqlite"), { verbose: console.log })

  // Create tables if they don't exist
  db.exec(`
    CREATE TABLE IF NOT EXISTS admins (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS teams (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL,
      max_members INTEGER DEFAULT 8,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      team_id INTEGER NOT NULL,
      username TEXT NOT NULL UNIQUE,
      device_token TEXT,
      last_active TIMESTAMP,
      registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      made_final_submission BOOLEAN DEFAULT 0,
      FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS challenges (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      short_name TEXT NOT NULL,
      points INTEGER DEFAULT 100,
      location_top_left_lat REAL,
      location_top_left_lng REAL,
      location_bottom_right_lat REAL,
      location_bottom_right_lng REAL,
      key_hash TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS solves (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      challenge_id INTEGER NOT NULL,
      solved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
      FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
      UNIQUE(user_id, challenge_id)
    );

    CREATE TABLE IF NOT EXISTS messages (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      team_id INTEGER,
      content TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS message_delivery (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      message_id INTEGER NOT NULL,
      user_id INTEGER NOT NULL,
      delivered BOOLEAN DEFAULT 0,
      delivered_at TIMESTAMP,
      FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
      FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
      UNIQUE(message_id, user_id)
    );
  `)

  // Insert sample data if database is empty
  const adminsCount = db.prepare("SELECT COUNT(*) as count FROM admins").get().count

  if (adminsCount === 0) {
    // Insert admin user
    db.prepare("INSERT INTO admins (username, password) VALUES (?, ?)").run("admin", "0af056adb0f926c3efd57d19ef3d6ca9:ee6585fabbe8ee80921d2fcf3ff9e2e7f7c2aab707b487ece2a6eebed3e048bdaee08fef3227bd404150bec24adf320fd2d58f5cde9b040a57119627002dd95d") // hello

    // Insert sample teams
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)").run(
      "Tech Wizards",
      "44fcb1a4247cc3892f52464eb97ca81f:09c9ee5294f0243a5d8dc7f850baa48e4ddebc169808c0a6f61616fe19f4bb0328ad91574e52c5b17c1cadb3352a8967427b6702785f43ce6d2af524d04d9f56",
      8
    ) // password123
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)").run(
      "Binary Bandits",
      "795f44c781cfde89c788538fe1fc610e:3cf2bc9a3a81a76ba25e8807a6cb1a961e0d08a14be631cb85d61dc780da3684a58e503e6eb92c42578d5507dc1fd4faf57b1c764d78c6b159d1b567e3768b97",
      8,
    ) // password123
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)").run(
      "Circuit Breakers",
      "d13395100f6618943f395f7cded24666:cb433ac9b5dc2d62f24bfd5cc4cf5614f6b979932a8228aa7950917974ad1c29738f3dc5e13ff3572ccc069ea53e376c18c93e86e48756318f9e97f0f73801d2",
      8,
    ) // password123

    db.prepare(`INSERT INTO challenges (name, short_name, points, location_top_left_lat, location_top_left_lng, location_bottom_right_lat, location_bottom_right_lng, key_hash) 
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run("Find the Beacon", "Beacon", 100, 48.8584, 2.2945, 48.8554, 2.2975, "a1b2c3d4e5f6g7h8i9j0")

    db.prepare(`
      INSERT INTO challenges (name, short_name, points, location_top_left_lat, location_top_left_lng, location_bottom_right_lat, location_bottom_right_lng, key_hash) 
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run("Decode the Signal", "Signal", 150, 48.8614, 2.3375, 48.8584, 2.3405, "b2c3d4e5f6g7h8i9j0k1")

    db.prepare(`
      INSERT INTO challenges (name, short_name, points, location_top_left_lat, location_top_left_lng, location_bottom_right_lat, location_bottom_right_lng, key_hash) 
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run("Capture the Flag", "CTF", 200, 48.8744, 2.2945, 48.8714, 2.2975, "c3d4e5f6g7h8i9j0k1l2")

    db.prepare(`
      INSERT INTO users (team_id, username, device_token, last_active)
      VALUES (?, ?, ?, CURRENT_TIMESTAMP),
             (?, ?, ?, CURRENT_TIMESTAMP)
    `).run(
      1, "alice", "token_alice_123",
      2, "bob",   "token_bob_456"
    );
    
    db.prepare(`
      INSERT INTO solves (user_id, challenge_id, solved_at)
      VALUES (?, ?, DATETIME('now', '-1 hour')),
             (?, ?, DATETIME('now', '-30 minutes')),
             (?, ?, DATETIME('now', '-2 hours'))
    `).run(
      1, 1,
      1, 2,
      2, 1
    );

    console.log("Sample data inserted")
  }
}

// Authentication middleware for admin routes
const authenticateAdmin = (req, res, next) => {
  const adminId = req.cookies.adminId

  if (!adminId) {
    return res.status(401).json({ error: "Unauthorized" })
  }

  try {
    const admin = db.prepare("SELECT id FROM admins WHERE id = ?").get(adminId)

    if (!admin) {
      return res.status(401).json({ error: "Unauthorized" })
    }

    req.adminId = adminId
    next()
  } catch (error) {
    console.error("Authentication error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
}

function hash_password(password) {
  const salt = crypto.randomBytes(16).toString("hex")
  const hash = crypto.pbkdf2Sync(password, salt, 1000, 64, "sha512").toString("hex")
  return `${salt}:${hash}`
}
function verify_password(storedPassword, inputPassword) {
  const [salt, hash] = storedPassword.split(":")
  const inputHash = crypto.pbkdf2Sync(inputPassword, salt, 1000, 64, "sha512").toString("hex")
  return hash === inputHash
}
// Middleware to check if the user is logged in
const authenticateUserWithCookie = (req, res, next) => {
  const deviceToken = req.cookies.deviceToken

  if (!deviceToken) {
    return res.status(401).json({ error: "Unauthorized" })
  }

  try {
    const user = db.prepare("SELECT id FROM users WHERE device_token = ?").get(deviceToken)

    if (!user) {
      return res.status(401).json({ error: "Unauthorized" })
    }

    req.userId = user.id
    next()
  } catch (error) {
    console.error("Authentication error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
}

// Middleware to check if the user is logged in

// Routes

// Admin login endpoint
app.post("/api/admin/login", (req, res) => {
  const { username, password } = req.body

  try {
    const admin = db.prepare("SELECT id, password FROM admins WHERE username = ?").get(username)

    if (!admin || !verify_password(admin.password, password)) {
      return res.status(401).json({ error: "Invalid credentials" })
    }

    // Set HTTP-only cookie with admin ID
    res.cookie("adminId", admin.id, {
      httpOnly: true,
      secure: false,
      maxAge: 60 * 60 * 1000, // 1 hour
      sameSite: "Lax",
    })

    res.json({ success: true })
  } catch (error) {
    console.error("Admin login error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

app.post("/api/admin/logout", (req, res) => {
  // res.clearCookie("adminId")
  res.deleteCookie("adminId")
  res.json({ success: true })
})

// Team login endpoint (for mobile app)
app.post("/api/team/login", (req, res) => {
  const { device_name, username, password } = req.body

  try {
    // Check if team exists and password is correct
    const team = db.prepare("SELECT id, password FROM teams WHERE username = ?").get(username)

    if (!team || verify_password(team.password, password)) {
      return res.status(401).json({ error: "WRONG_CREDS" })
    }

    // Check if team has reached max members
    const userCount = db.prepare("SELECT COUNT(*) as count FROM users WHERE team_id = ?").get(teamId).count

    if (userCount && userCount >= team.max_members) {
      return res.status(403).json({ error: "ACCOUNT_LIMIT_REACHED" })
    }

    if ( !device_name ) // randomly generate a device name
      device_name = `Device_${crypto.randomBytes(4).toString("hex")}`
    
    // Create new user
    const deviceToken = crypto.randomBytes(32).toString("hex")
    const result = db
      .prepare("INSERT INTO users (team_id, username, device_token, last_active) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")
      .run(teamId, device_name, deviceToken)

    // Get challenges
    const challenges = db
      .prepare(`
      SELECT id, name, short_name, points, 
             location_top_left_lat, location_top_left_lng, 
             location_bottom_right_lat, location_bottom_right_lng, 
             key_hash
      FROM challenges
    `)
      .all()

    res.json({
      deviceToken,
      challenges: challenges.map((c) => ({
        id: c.id,
        name: c.name,
        shortName: c.short_name,
        points: c.points,
        location: {
          topLeft: { lat: c.location_top_left_lat, lng: c.location_top_left_lng },
          bottomRight: { lat: c.location_bottom_right_lat, lng: c.location_bottom_right_lng },
        },
        keyHash: c.key_hash,
      })),
      serverTime: Date.now(),
    })
  } catch (error) {
    console.error("Team login error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Update leaderboard endpoint (for mobile app)
app.post("/api/update-leaderboard", (req, res) => {
  const { deviceToken, solves, isFinalSubmission } = req.body

  try {
    // Verify device token
    const user = db.prepare("SELECT * FROM users WHERE device_token = ?").get(deviceToken)

    if (!user) {
      return res.status(401).json({ error: "Invalid device token" })
    }

    const user_status = db
      .prepare("SELECT made_final_submission FROM users WHERE user_id = ?")
      .get(user.id)

    if (!user_status.made_final_submission) {
      return res.status(403).json({ error: "Already made final submission" })
    }

    const curr_team = db
      .prepare("SELECT id FROM users WHERE team_id = ?")
      .all(user.team_id)
    const users_in_curr_team = curr_team.map((user) => user.id)

    const updateSolves = db.transaction((solves) => {
      for (const solve of solves) {
        // Check if chall exists, solve already exists aka me or someone else in the team solved it
        const placeholders = users_in_curr_team.map(() => "?").join(", ")
        const query = `SELECT * FROM solves WHERE user_id IN (${placeholders}) AND challenge_id = ?`
        const existingSolve = db.prepare(query).get(...users_in_curr_team, solve.challengeId)
        
        if ( !existingSolve )
          db.prepare("INSERT INTO solves (user_id, challenge_id) VALUES (?, ?)").run(
            user.id,
            solve.challengeId,
          )
      }
    });

    // Execute the transaction
    updateSolves(solves)

    // Update last active timestamp
    db.prepare("UPDATE users SET last_active = CURRENT_TIMESTAMP WHERE id = ?").run(user.id)
    if ( isFinalSubmission ) {
      db.prepare("UPDATE users SET made_final_submission = 1 WHERE id = ?").run(user.id)
    }

    res.json({ success: true, serverTime: Date.now() })
  } catch (error) {
    console.error("Update leaderboard error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Get messages endpoint (for mobile app)
app.get("/api/messages", (req, res) => {
  const { deviceToken } = req.query

  try {
    // Verify device token
    const user = db.prepare("SELECT * FROM users WHERE device_token = ?").get(deviceToken)

    if (!user) {
      return res.status(401).json({ error: "Invalid device token" })
    }

    // Get undelivered messages for this user
    const messages = db
      .prepare(`
      SELECT m.id, m.content, m.created_at
      FROM messages m
      JOIN message_delivery md ON m.id = md.message_id
      WHERE md.user_id = ? AND md.delivered = 0
      ORDER BY m.created_at ASC
    `)
      .all(user.id)

    // Use a transaction for marking messages as delivered
    const markMessagesDelivered = db.transaction((messages) => {
      for (const message of messages) {
        db.prepare(`
          UPDATE message_delivery
          SET delivered = 1, delivered_at = CURRENT_TIMESTAMP
          WHERE message_id = ? AND user_id = ?
        `).run(message.id, user.id)
      }
    })

    // Execute the transaction
    markMessagesDelivered(messages)

    // Update last active timestamp
    db.prepare("UPDATE users SET last_active = CURRENT_TIMESTAMP WHERE id = ?").run(user.id)

    res.json({
      messages: messages.map((m) => ({
        id: m.id,
        content: m.content,
        createdAt: m.created_at,
      })),
      serverTime: Date.now(),
    })
  } catch (error) {
    console.error("Get messages error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Protected admin routes
// Get teams (admin only)
app.get("/api/admin/teams", authenticateAdmin, (req, res) => {
  try {
    const teams = db
      .prepare(`
      SELECT 
        t.id, 
        t.name, 
        t.max_members as maxMembers,
        t.created_at as createdAt,
        COUNT(u.id) as memberCount
      FROM teams t
      LEFT JOIN users u ON t.id = u.team_id
      GROUP BY t.id
    `)
      .all()

    res.json({ teams })
  } catch (error) {
    console.error("Get teams error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Create team (admin only)
app.post("/api/admin/teams", authenticateAdmin, (req, res) => {
  const { name, password, maxMembers } = req.body

  try {
    if ( !name || !password || !maxMembers ) {
      return res.status(400).json({ error: "Missing required fields" })
    }
    // Check if team name already exists
    const existingTeam = db.prepare("SELECT id FROM teams WHERE name = ?").get(name)

    if (existingTeam) {
      return res.status(400).json({ error: "Team name already exists" })
    }

    password = hash_password(password)
    const result = db
      .prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run(name, password, maxMembers)

    const newTeamDetais = db
      .prepare("SELECT id, name, max_members as maxMembers, created_at as createdAt FROM teams WHERE id = ?")
      .get(result.lastInsertRowid)

    res.status(201).json({
      ...newTeamDetais,
      memberCount: 0,
    })
  } catch (error) {
    console.error("Create team error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Get team details (admin only)
app.get("/api/admin/teams/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params

  try {
    const team = db.prepare("SELECT id, name, max_members as maxMembers, created_at as createdAt FROM teams WHERE id = ?").get(id)

    if (!team) {
      return res.status(404).json({ error: "Team not found" })
    }

    const members = db
      .prepare(`
      SELECT id, username, last_active as lastActive, registered_at as registeredAt, made_final_submission as hasSubmitted
      FROM users
      WHERE team_id = ?
    `)
      .all(id)

    const solves = db
      .prepare(`
      SELECT 
        c.id,
        c.name,
        c.short_name as shortName,
        s.solved_at as solvedAt,
        u.username as solvedBy 
      FROM challenges c
      LEFT JOIN (
        SELECT challenge_id, MIN(solved_at) as solved_at, user_id
        FROM solves
        WHERE user_id IN (SELECT id FROM users WHERE team_id = ?)
        GROUP BY challenge_id
      ) s ON c.id = s.challenge_id
      LEFT JOIN users u ON s.user_id = u.id
    `)
      .all(id)

    res.json({
      team,
      members,
      solves,
    })
  } catch (error) {
    console.error("Get team details error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Update details of a team (admin only)
app.put("/api/admin/teams/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params
  const { password, maxMembers } = req.body

  try {
    if ( !password && !maxMembers ) {
      return res.status(400).json({ error: "No fields to update" })
    }

    const existingTeam = db.prepare("SELECT * FROM teams WHERE id = ?").get(id)

    if (!existingTeam) {
      return res.status(404).json({ error: "Team not found" })
    }

    if ( !password )
      password = existingTeam.password
    else
      password = hash_password(password)
    if ( !maxMembers )
      maxMembers = existingTeam.max_members

    // Update team details
    const result = db
      .prepare("UPDATE teams SET password = ?, max_members = ? WHERE id = ?")
      .run(password, maxMembers, id)

    if ( result.changes === 0 ) {
      return res.status(400).json({ error: "Failed to update team" })
    }
    
    return res.status(200).json();
  } catch (error) {
    console.error("Update team error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Delete team (admin only)
app.delete("/api/admin/teams/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params

  try {
    // Check if team exists
    const existingTeam = db.prepare("SELECT id FROM teams WHERE id = ?").get(id)

    if (!existingTeam) {
      return res.status(404).json({ error: "Team not found" })
    }

    // Delete team and all associated users and solves
    db.prepare("DELETE FROM teams WHERE id = ?").run(id)

    res.status(204).send()
  } catch (error) {
    console.error("Delete team error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

app.post('/api/admin/users/:id/force-submit', authenticateAdmin, (req, res) => {
  const { id } = req.params

  try {
    const result = db
      .prepare("UPDATE users SET made_final_submission = true WHERE id = (?)")
      .run(id)
    
      console.log(result)
    return res.status(200).json({ 
      success: 'Yayayay'
    });
  } catch (error) {
    console.error("Send message error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Send message (admin only)
app.post("/api/admin/send-message", authenticateAdmin, (req, res) => {
  const { teamId, content } = req.body

  try {
    // Use a transaction for sending messages
    const sendMessage = db.transaction((teamId, content) => {
      // Insert message
      const result = db
        .prepare("INSERT INTO messages (team_id, content) VALUES (?, ?)")
        .run(teamId === "all" ? null : teamId, content)

      const messageId = result.lastInsertRowid

      // Get users to deliver to
      let users
      if (teamId === "all") {
        users = db.prepare("SELECT id FROM users").all()
      } else {
        users = db.prepare("SELECT id FROM users WHERE team_id = ?").all(teamId)
      }

      // Create message delivery entries
      const insertDelivery = db.prepare("INSERT INTO message_delivery (message_id, user_id) VALUES (?, ?)")
      for (const user of users) {
        insertDelivery.run(messageId, user.id)
      }

      return messageId
    })

    // Execute the transaction
    const messageId = sendMessage(teamId, content)

    res.json({ success: true, messageId })
  } catch (error) {
    console.error("Send message error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Get leaderboard (public)
app.get("/api/leaderboard", (req, res) => {
  try {
    // Get teams with solve counts
    const teams = db
      .prepare(`
      SELECT 
        t.id, 
        t.name,
        (
          SELECT SUM(c.points)
          FROM solves s
          JOIN users u2 ON s.user_id = u2.id
          JOIN challenges c ON s.challenge_id = c.id
          WHERE u2.team_id = t.id
          GROUP BY u2.team_id
        ) as total_points
      FROM teams t
      ORDER BY total_points DESC
    `)
      .all()

    // Get challenges
    const challenges = db.prepare("SELECT id, name, short_name, points FROM challenges").all()

    // Get solves for each team
    const teamSolves = []
    for (const team of teams) {
      const solves = db
        .prepare(`
        SELECT 
          c.id as challenge_id,
          MIN(s.solved_at) as first_solved_at
        FROM solves s
        JOIN users u ON s.user_id = u.id
        JOIN challenges c ON s.challenge_id = c.id
        WHERE u.team_id = ?
        GROUP BY c.id
      `)
        .all(team.id)

      teamSolves.push({
        teamId: team.id,
        name: team.name,
        solves: solves.map((s) => ({
          challengeId: s.challenge_id,
          timestamp: new Date(s.first_solved_at).getTime(),
          solved: true,
        })),
        totalPoints: team.total_points || 0,
      })
    }

    const finalSubmissionsLeft = db
    .prepare(`
    SELECT COUNT(*) as count
    FROM users
    WHERE made_final_submission = 0
  `)
    .get()
  const competitionEnded = finalSubmissionsLeft.count === 0

    res.json({
      challenges: challenges.map((c) => ({
        id: c.id,
        name: c.name,
        shortName: c.short_name,
        points: c.points,
      })),
      competitionEnded: competitionEnded,
      teamSolves,
    })
  } catch (error) {
    console.error("Get leaderboard error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Get all challenges (admin only)
app.get("/api/admin/challenges", authenticateAdmin, (req, res) => {
  try {
    const challenges = db
      .prepare(`
      SELECT 
        id, 
        name, 
        short_name as shortName, 
        points,
        location_top_left_lat,
        location_top_left_lng,
        location_bottom_right_lat,
        location_bottom_right_lng,
        key_hash as keyHash
      FROM challenges
    `)
      .all()

    // Format the response to match the frontend expectations
    const formattedChallenges = challenges.map((c) => ({
      id: c.id.toString(),
      name: c.name,
      shortName: c.shortName,
      points: c.points,
      location: {
        topLeft: { lat: c.location_top_left_lat, lng: c.location_top_left_lng },
        bottomRight: { lat: c.location_bottom_right_lat, lng: c.location_bottom_right_lng },
      },
      keyHash: c.keyHash,
    }))

    res.json({ challenges: formattedChallenges })
  } catch (error) {
    console.error("Get challenges error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Get a single challenge (admin only)
app.get("/api/admin/challenges/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params

  try {
    const challenge = db
      .prepare(`
      SELECT 
        id, 
        name, 
        short_name as shortName, 
        points,
        location_top_left_lat,
        location_top_left_lng,
        location_bottom_right_lat,
        location_bottom_right_lng,
        key_hash as keyHash
      FROM challenges
      WHERE id = ?
    `)
      .get(id)

    if (!challenge) {
      return res.status(404).json({ error: "Challenge not found" })
    }

    // Format the response to match the frontend expectations
    const formattedChallenge = {
      id: challenge.id.toString(),
      name: challenge.name,
      shortName: challenge.shortName,
      points: challenge.points,
      location: {
        topLeft: { lat: challenge.location_top_left_lat, lng: challenge.location_top_left_lng },
        bottomRight: { lat: challenge.location_bottom_right_lat, lng: challenge.location_bottom_right_lng },
      },
      keyHash: challenge.keyHash,
    }

    res.json(formattedChallenge)
  } catch (error) {
    console.error("Get challenge error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// // Create a new challenge (admin only)
app.post("/api/admin/challenges", authenticateAdmin, (req, res) => {
  const { name, shortName, points, location, keyHash } = req.body

  if (!name || !shortName || !points || !location || !keyHash) {
    return res.status(400).json({ error: "Missing required fields" })
  }

  try {
    const result = db
      .prepare(`
      INSERT INTO challenges (
        name, 
        short_name, 
        points, 
        location_top_left_lat, 
        location_top_left_lng, 
        location_bottom_right_lat, 
        location_bottom_right_lng, 
        key_hash
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `)
      .run(
        name,
        shortName,
        points,
        location.topLeft.lat,
        location.topLeft.lng,
        location.bottomRight.lat,
        location.bottomRight.lng,
        keyHash,
      )

    const newChallengeId = result.lastInsertRowid

    // Return the created challenge
    const newChallenge = {
      id: newChallengeId.toString(),
      name,
      shortName,
      points,
      location,
      keyHash,
    }

    res.status(201).json(newChallenge)
  } catch (error) {
    console.error("Create challenge error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// // Update a challenge (admin only)
app.put("/api/admin/challenges/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params
  const { name, shortName, points, location, keyHash } = req.body

  if (!name && !shortName && !points && !location && !keyHash) {
    return res.status(400).json({ error: "No fields to update" })
  }

  try {
    // First, check if the challenge exists
    const existingChallenge = db.prepare("SELECT id FROM challenges WHERE id = ?").get(id)

    if (!existingChallenge) {
      return res.status(404).json({ error: "Challenge not found" })
    }

    // Build the update query dynamically based on provided fields
    let updateQuery = "UPDATE challenges SET "
    const updateParams = []
    const updateFields = []

    if (name) {
      updateFields.push("name = ?")
      updateParams.push(name)
    }

    if (shortName) {
      updateFields.push("short_name = ?")
      updateParams.push(shortName)
    }

    if (points) {
      updateFields.push("points = ?")
      updateParams.push(points)
    }

    if (location) {
      if (location.topLeft && location.topLeft.lat) {
        updateFields.push("location_top_left_lat = ?")
        updateParams.push(location.topLeft.lat)
      }
      if (location.topLeft && location.topLeft.lng) {
        updateFields.push("location_top_left_lng = ?")
        updateParams.push(location.topLeft.lng)
      }
      if (location.bottomRight && location.bottomRight.lat) {
        updateFields.push("location_bottom_right_lat = ?")
        updateParams.push(location.bottomRight.lat)
      }
      if (location.bottomRight && location.bottomRight.lng) {
        updateFields.push("location_bottom_right_lng = ?")
        updateParams.push(location.bottomRight.lng)
      }
    }

    if (keyHash) {
      updateFields.push("key_hash = ?")
      updateParams.push(keyHash)
    }

    updateQuery += updateFields.join(", ") + " WHERE id = ?"
    updateParams.push(id)

    // Execute the update
    db.prepare(updateQuery).run(...updateParams)

    // Get the updated challenge
    const updatedChallenge = db
      .prepare(`
      SELECT 
        id, 
        name, 
        short_name as shortName, 
        points,
        location_top_left_lat,
        location_top_left_lng,
        location_bottom_right_lat,
        location_bottom_right_lng,
        key_hash as keyHash
      FROM challenges
      WHERE id = ?
    `)
      .get(id)

    // Format the response
    const formattedChallenge = {
      id: updatedChallenge.id.toString(),
      name: updatedChallenge.name,
      shortName: updatedChallenge.shortName,
      points: updatedChallenge.points,
      location: {
        topLeft: { lat: updatedChallenge.location_top_left_lat, lng: updatedChallenge.location_top_left_lng },
        bottomRight: {
          lat: updatedChallenge.location_bottom_right_lat,
          lng: updatedChallenge.location_bottom_right_lng,
        },
      },
      keyHash: updatedChallenge.keyHash,
    }

    res.json(formattedChallenge)
  } catch (error) {
    console.error("Update challenge error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// // Delete a challenge (admin only)
app.delete("/api/admin/challenges/:id", authenticateAdmin, (req, res) => {
  const { id } = req.params

  try {
    const existingChallenge = db.prepare("SELECT id FROM challenges WHERE id = ?").get(id)

    if (!existingChallenge) {
      return res.status(404).json({ error: "Challenge not found" })
    }

    db.prepare("DELETE FROM challenges WHERE id = ?").run(id)

    res.status(204).send()
  } catch (error) {
    console.error("Delete challenge error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

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

