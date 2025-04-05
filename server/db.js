const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

// Define the path to the database file
const dbPath = path.join(__dirname, 'database.db');

// Function to check if the database exists and create it if not
function initializeDatabase() {
  if (!fs.existsSync(dbPath)) {
    const db = new sqlite3.Database(dbPath, (err) => {
      if (err) {
        console.error('Error opening database:', err);
        process.exit(1);
      } else {
        console.log('Database created.');

        db.serialize(() => {
          db.run(`CREATE TABLE Teams (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE,
            password TEXT,
            sessions_allowed_count INTEGER
          )`, (err) => {
            if (err) {
              console.error("Error creating Teams table:", err);
              process.exit(1);
            }
          });

          db.run(`CREATE TABLE Sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            team_id INTEGER,
            FOREIGN KEY (team_id) REFERENCES Teams(id)
          )`, (err) => {
            if (err) {
              console.error("Error creating Sessions table:", err);
              process.exit(1);
            }
          });

          db.run(
            `INSERT INTO Teams (name, password, sessions_allowed_count) VALUES (?, ?, ?)`,
            ["what_a_name", "what_a_password", 2],
            (err) => {
              if (err) {
                console.error("Error inserting initial team:", err);
                process.exit(1);
              }
            }
          );
        });
      }
    });
  } else {
    console.log('Database already exists.');
  }
  console.log('returning from initializeDatabase...');
}

// Open the SQLite database connection and return it
function getDatabaseConnection() {
  const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
      console.error('Error opening database:', err);
      process.exit(1);
    }
  });
  console.log('returning from getDatabaseConnection...');
  return db;
}

module.exports = {
  initializeDatabase,
  getDatabaseConnection,
};

