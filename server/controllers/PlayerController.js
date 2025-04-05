const express = require('express');
const { getDatabaseConnection } = require('../db');

class DBOps_Wrapper {
  constructor() {
    this.db = getDatabaseConnection();
  }

  async verifyLoginCreds(username, password) {
    try {
      const row = await new Promise((resolve, reject) => {
        this.db.get(
          `SELECT * FROM Teams WHERE name = ?`,
          [username],
          (err, row) => {
            if (err) return reject(err);
            resolve(row);
          }
        );
      });

      if (!row) return "INVALID_USERNAME";
      if (password !== row.password) return "INCORRECT_PASSWORD";

      const countResult = await new Promise((resolve, reject) => {
        this.db.get(
          `SELECT COUNT(*) as count FROM Sessions WHERE team_id = ?`,
          [row.id],
          (err, result) => {
            if (err) return reject(err);
            resolve(result);
          }
        );
      });

      if (row.sessions_allowed_count === countResult.count) {
        return "SESSIONS_FULL";
      }
      await new Promise((resolve, reject) => {
        this.db.run(
          `INSERT INTO Sessions (team_id) VALUES (?)`,
          [row.id],
          (err) => {
            if (err) return reject(err);
            resolve();
          }
        );
      });

      return "SUCCESS";
    } catch (err) {
      console.error("Database error:", err);
      return "INVALID_USERNAME";
    }
  }
}

class PlayerController {
  constructor() {
    this.db_wrapper = new DBOps_Wrapper();
    this.router = express.Router();
    this.registerRoutes();
  }

  registerRoutes() {
    this.router.post('/login', this.loginUser.bind(this));
  }

  async loginUser(req, res) {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ error: 'Both username and password are required' });
    }
    
    try {
      const ret_val = await this.db_wrapper.verifyLoginCreds(username, password);
      if (ret_val === "SUCCESS") return res.status(200).json({});
      
      const error_map = {
        "INVALID_USERNAME": "User not found",
        "INCORRECT_PASSWORD": "Incorrect password",
        "SESSIONS_FULL": "No more sessions allowed for this team!"
      };
      return res.status(401).json({ error: error_map[ret_val] });
    } catch (err) {
      console.error("Login error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
}

module.exports = PlayerController;

