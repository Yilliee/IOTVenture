import express from "express"
import crypto from "crypto"
import cors from "cors"
import path from "path"
import bodyParser from "body-parser"
import cookieParser from "cookie-parser"
import { db } from "./db.js"
import { hash_password, verify_password } from "./util.js"

// Initialize express app
const app = express()

app.use(
  cors({
    origin: process.env.IOTVENTURE_FRONTEND_URL || "http://localhost:3000",
    credentials: true,
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"]
  }),
)
app.use(bodyParser.json())
app.use(cookieParser())

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

// Middleware to check if the user is logged in
const authenticateUser = (req, res, next) => {
  const deviceToken = req.body.deviceToken || req.query.deviceToken

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

// Admin logout endpoint
app.post("/api/admin/logout", authenticateAdmin, (req, res) => {
  res.clearCookie("adminId")
  res.json({ success: true })
})

// Team login endpoint (for mobile app)
app.post("/api/team/login", (req, res) => {
  let { device_name, username, password } = req.body

  try {
    // Check if team exists and password is correct
    const team = db.prepare("SELECT id, password FROM teams WHERE name = ?").get(username)
    
    if (!team || !verify_password(team.password, password)) {
      return res.status(401).json({ error: "WRONG_CREDS" })
    }

    // Check if team has reached max members
    const userCount = db.prepare("SELECT COUNT(*) as count FROM users WHERE team_id = ?").get(team.id).count

    if (userCount && userCount >= team.max_members) {
      return res.status(403).json({ error: "ACCOUNT_LIMIT_REACHED" })
    }

    if ( !device_name ) // randomly generate a device name
      device_name = `Device_${crypto.randomBytes(4).toString("hex")}`

    // Generate device token
    const deviceToken = crypto.randomBytes(32).toString("hex")
    
    db.prepare("INSERT INTO users (team_id, username, device_token, last_active) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")
      .run(team.id, device_name, deviceToken)

    // Get challenges
    const challenges = db
      .prepare(`
      SELECT id, name, short_name as shortName, points, 
             location_top_left_lat as "location.topLeft.lat", 
             location_top_left_lng as "location.topLeft.lng", 
             location_bottom_right_lat as "location.bottomRight.lat", 
             location_bottom_right_lng as "location.bottomRight.lng", 
             key_hash as keyHash
      FROM challenges
    `)
      .all()

    // Format challenges to match the expected response
    const formattedChallenges = challenges.map(challenge => ({
      id: challenge.id,
      name: challenge.name,
      shortName: challenge.shortName,
      points: challenge.points,
      location: {
        topLeft: {
          lat: challenge["location.topLeft.lat"],
          lng: challenge["location.topLeft.lng"]
        },
        bottomRight: {
          lat: challenge["location.bottomRight.lat"],
          lng: challenge["location.bottomRight.lng"]
        }
      },
      keyHash: challenge.keyHash
    }))

    res.json({
      deviceToken,
      challenges: formattedChallenges,
      serverTime: Date.now()
    })
  } catch (error) {
    console.error("Team login error:", error)
    res.status(500).json({ error: "Internal server error" })
  }
})

// Update leaderboard endpoint (for mobile app)
app.post("/api/update-leaderboard", authenticateUser, (req, res) => {
  const { deviceToken, solves, isFinalSubmission } = req.body


  try {
    // Verify device token
    const user = db.prepare("SELECT * FROM users WHERE device_token = ?").get(deviceToken)

    if (!user) {
      return res.status(401).json({ error: "Invalid device token" })
    }

    const user_status = db
      .prepare("SELECT made_final_submission FROM users WHERE id = ?")
      .get(user.id)

    if (user_status.made_final_submission) {
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
        
        if ( !existingSolve ) {
          db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, ?)").run(
            user.id,
            solve.challengeId,
            solve.solvedAt
          )
        }
        else if ( existingSolve.solved_at > solve.solvedAt ) {
          db.prepare("UPDATE solves SET solved_at = ?, user_id = ? WHERE user_id = ? AND challenge_id = ?").run(
            solve.solvedAt,
            user.id,
            existingSolve.user_id,
            existingSolve.challengeId,
          )
        }
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

// Get messages endpoint (for mobile app)
app.get("/api/messages", authenticateUser, (req, res) => {
  const deviceToken = req.body.deviceToken || req.query.deviceToken

  if (!deviceToken) {
    return res.status(401).json({ error: "No device token provided" })
  }

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
  let { name, password, maxMembers } = req.body

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
  let{ password, maxMembers } = req.body

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

export default app;
