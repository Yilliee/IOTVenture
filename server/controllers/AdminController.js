const express = require('express');
const { getDatabaseConnection } = require('../db');

class DBOps_Wrapper {
  constructor() {
    this.db = getDatabaseConnection();
  }

  async registerTeam(name, password, sessions_allowed_count) {
    try {
      const row = await new Promise((resolve, reject) => {
        this.db.get(
          `SELECT * FROM Teams WHERE name = ?`,
          [name],
          (err, row) => {
            if (err) return reject(err);
            resolve(row);
          }
        );
      });
      if (row) return "ALREADY_ALLOCATED_USERNAME";

      await new Promise((resolve, reject) => {
        this.db.run(
          `INSERT INTO Teams (name, password, sessions_allowed_count) VALUES (?, ?, ?)`,
          [name, password, sessions_allowed_count],
          (err) => {
            if (err) return reject(err);
            resolve();
          }
        );
      });
      return "SUCCESS";
    } catch (err) {
      console.error("Database error:", err);
      return "DATABASE_ERROR";
    }
  }
}

class AdminController {
  constructor() {
    this.db_wrapper = new DBOps_Wrapper();
    this.router = express.Router();
    this.registerRoutes();
  }

  registerRoutes() {
    this.router.post('/register', this.registerTeam.bind(this));
  }
  async registerTeam(req, res) {
    const DEFAULT_SESSIONS_COUNT = 4; 
    let { name, password, sessions_allowed_count } = req.body;
    if (!name || !password) return res.status(400).json({ error: 'Invalid username or password' });
    if (!sessions_allowed_count) sessions_allowed_count = DEFAULT_SESSIONS_COUNT;
    else if (sessions_allowed_count <= 0) return res.status(400).json({ error: `Invalid sessions count! Either give valid sessions count or dont send so we can fall back to default(${DEFAULT_SESSIONS_COUNT})` });
    if (!sessions_allowed_count) sessions_allowed_count = DEFAULT_SESSIONS_COUNT;
    //if (!name || !password || !sessions_allowed_count) {
    //      const substr = '';
    //      [name, password, sessions_allowed_count].forEach((el, idx) => {
    //        if (el && substr.length > 0) {
    //          substr += `, ${el}`;
    //        } else if (el) {
    //          substr += `${el}`;
    //        }
    //      });
    //      return res.status(400).json({ error: `${substr} required` });
    //}

    try {
      const ret_val = await this.db_wrapper.registerTeam(name, password, sessions_allowed_count);
      if (ret_val === "SUCCESS") return res.status(200).json({});
      
      const error_map = {
        "ALREADY_ALLOCATED_USERNAME": "Choose unique name for your team!",
        "DATABASE_ERROR": "Failed to register(unknown reason), try again",
      };
      return res.status(401).json({ error: error_map[ret_val] });
    } catch (err) {
      console.error("Registration error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
}

module.exports = AdminController;

