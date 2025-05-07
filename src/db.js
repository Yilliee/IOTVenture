import Database from "better-sqlite3"
import crypto from "crypto"

const db = new Database("database.sqlite", process.env.IOTVENTURE_BACKEND_DEBUG ? { verbose: console.log } : {});


function initializeDatabase() {
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

export { db, initializeDatabase };

