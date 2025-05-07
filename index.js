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

// Middleware
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
    db.prepare("INSERT INTO admins (username, password) VALUES (?, ?)").run(
      "admin", "0af056adb0f926c3efd57d19ef3d6ca9:ee6585fabbe8ee80921d2fcf3ff9e2e7f7c2aab707b487ece2a6eebed3e048bdaee08fef3227bd404150bec24adf320fd2d58f5cde9b040a57119627002dd95d"
    ) // hello

    // Insert sample teams
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run("Tech Wizards", "c49b7d937237b5884a9cb1c7cea0cb28:33afb21dace89ce0baab12cdda282463dfb4793f1e13361c3176b348bcfc01d05016c8e116c8d879e71c1997be70fa285722e83da7cae0f0578d5049026693e7", 8) // password123
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run("Code Ninjas", "cfede10dbe2a041d7d4133312ecb17e6:d394804351ee0aa5585438ff0f8665b551f096695733ad8f3524ff4b7ff9e4f60ae7e25bc25b794cde7068c71901baf04e56ca8d137280e437511e250a064e07", 8) // ninja456
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run("Byte Squad", "0c1fcf019e665a98867a8ce17a9b4aa2:474fb33bf13625d488452e9254b9e851a6e1b088c28ec7a266eeffaed0d72cc6ffd4aa6b228e9862b5922b2526b5a54dd3a0f636c468b4667da2a88cd9e3fcc1", 8) // byte789
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run("Pixel Warriors", "04474e29dab94f50eaabe5e06856eca5:1b87fca1d777573e18caf97ec2ee716de936e78b163cce0e3ee2740e95bd1c940c8edf5dbd757d2bd4d15c273419066af75c424dc96fbbf325323de6e4e06fa6", 8) // pixel123
    db.prepare("INSERT INTO teams (name, password, max_members) VALUES (?, ?, ?)")
      .run("Binary Masters", "9a1160a0ab163637f1748370f3b0dab3:c9d14c979e48b5f0a1008fe85e1972187c06f556b060d7439c5ee51431a7c1d0f9c1deef70a7a7bfa6511439b4b2fccd7c02c1d00b64b7aee67642fec8b4e7fd", 8) // binary456

    // Insert sample challenges
    const challenges = [
      {
        name: "Find the Beacon",
        short_name: "Beacon",
        points: 100,
        lat_top_left: 31.5204,
        lng_top_left: 74.3587,
        lat_bottom_right: 31.5174,
        lng_bottom_right: 74.3617,
        key_hash: "E74E9C8A" // NFC card hash
      },
      {
        name: "Decode the Signal",
        short_name: "Signal",
        points: 150,
        lat_top_left: 31.5234,
        lng_top_left: 74.3375,
        lat_bottom_right: 31.5204,
        lng_bottom_right: 74.3405,
        key_hash: "83D45519"
      },
      {
        name: "Capture the Flag",
        short_name: "CTF",
        points: 200,
        lat_top_left: 31.5344,
        lng_top_left: 74.2945,
        lat_bottom_right: 31.5314,
        lng_bottom_right: 74.2975,
        key_hash: "A7FE30C7"
      },
      {
        name: "Hack the Network",
        short_name: "Network",
        points: 175,
        lat_top_left: 31.5244,
        lng_top_left: 74.3245,
        lat_bottom_right: 31.5214,
        lng_bottom_right: 74.3275,
        key_hash: "5C03C770"
      },
      {
        name: "Crack the Code",
        short_name: "Code",
        points: 125,
        lat_top_left: 31.5284,
        lng_top_left: 74.3145,
        lat_bottom_right: 31.5254,
        lng_bottom_right: 74.3175,
        key_hash: "e5f6g7h8i9j0k1l2m3n4"
      }
    ];

    const insertChallenge = db.prepare(`
      INSERT INTO challenges (name, short_name, points, location_top_left_lat, location_top_left_lng, 
                            location_bottom_right_lat, location_bottom_right_lng, key_hash) 
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);

    challenges.forEach(challenge => {
      insertChallenge.run(
        challenge.name,
        challenge.short_name,
        challenge.points,
        challenge.lat_top_left,
        challenge.lng_top_left,
        challenge.lat_bottom_right,
        challenge.lng_bottom_right,
        challenge.key_hash
      );
    });

    // Insert sample users for each team
    const teams = db.prepare("SELECT id FROM teams").all();
    teams.forEach(team => {
      const deviceToken = crypto.randomBytes(32).toString("hex");
      db.prepare("INSERT INTO users (team_id, username, device_token, last_active) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")
        .run(team.id, `user_${team.id}`, deviceToken);
    });

    // Insert sample solves
    const users = db.prepare("SELECT id FROM users").all();
    const challengeIds = db.prepare("SELECT id FROM challenges").all();
    
    // Tech Wizards solved all challenges
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-5 hours'))")
      .run(users[0].id, challengeIds[0].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-4 hours'))")
      .run(users[0].id, challengeIds[1].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-3 hours'))")
      .run(users[0].id, challengeIds[2].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-2 hours'))")
      .run(users[0].id, challengeIds[3].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-1 hour'))")
      .run(users[0].id, challengeIds[4].id);

    // Code Ninjas solved 4 challenges
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-4.5 hours'))")
      .run(users[1].id, challengeIds[0].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-3.5 hours'))")
      .run(users[1].id, challengeIds[1].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-2.5 hours'))")
      .run(users[1].id, challengeIds[2].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-1.5 hours'))")
      .run(users[1].id, challengeIds[3].id);

    // Byte Squad solved 3 challenges
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-4 hours'))")
      .run(users[2].id, challengeIds[0].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-3 hours'))")
      .run(users[2].id, challengeIds[1].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-2 hours'))")
      .run(users[2].id, challengeIds[2].id);

    // Pixel Warriors solved 2 challenges
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-3.5 hours'))")
      .run(users[3].id, challengeIds[0].id);
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-2.5 hours'))")
      .run(users[3].id, challengeIds[1].id);

    // Binary Masters solved 1 challenge
    db.prepare("INSERT INTO solves (user_id, challenge_id, solved_at) VALUES (?, ?, datetime('now', '-3 hours'))")
      .run(users[4].id, challengeIds[0].id);

    // Insert sample messages
    const messages = [
      { team_id: 1, content: "Found the first beacon!" },
      { team_id: 2, content: "Working on the signal challenge" },
      { team_id: 3, content: "Network challenge is tricky" },
      { team_id: 4, content: "Just solved the code challenge" },
      { team_id: 5, content: "CTF challenge completed!" }
    ];

    const insertMessage = db.prepare("INSERT INTO messages (team_id, content) VALUES (?, ?)");
    messages.forEach(message => {
      insertMessage.run(message.team_id, message.content);
    });

    console.log("Sample data inserted");
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

// Admin logout endpoint
app.post("/api/admin/logout", (req, res) => {
  res.clearCookie("adminId")
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
    
    // Create or update user
    userId = existingUser.id

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
app.get("/api/messages", (req, res) => {
  const { deviceToken } = req.query

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

// Start server
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

