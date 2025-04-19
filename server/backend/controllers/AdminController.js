const express = require('express');
const { getDatabaseConnection } = require('../db');

class DBOps_Wrapper {
  constructor() {
    this.db = getDatabaseConnection();
  }
  async readTeams() {
    try {
      const rows = await new Promise((resolve, reject) => {
        this.db.get(
          `SELECT * FROM Teams`,
          [],
          (err, rows) => {
            if (err) return reject(err);
            resolve(rows);
          }
        )
      });
      return ["SUCCESS", rows];
    } catch (err) {
      console.error(`database error: ${err}`);
      return ["DATABASE_ERROR", null];``
    }
  }
  async updateTeam(name, new_name, new_password, new_sessions_allowed_count) {
    try {
      const row = await new Promise((resolve, reject) => {
        this.db.get(
          `SELECT * FROM Teams WHERE name = ?`,
          [name],
          (err, row) => {
            if (err) return reject(err);
            resolve(row);
          }
        )
      });
      if (!row) return "INVALID_USERNAME";
      let updates=[];
      let params=[];
      [[new_name, "name = ?"], [new_password, "password = ?"], [new_sessions_allowed_count, "sessions_allowed_count = ?"]].forEach(([param, update]) => {
        if (param) {
          updates.push(update);
          params.push(param);
        }
      });
      params.push(name);
      await new Promise((resolve, reject) => {
        this.db.run(
          `UPDATE Teams SET ${updates.join(', ')} WHERE name = ?`,
          params,
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
  async killTeam(name, password) {
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

      if (!row) return "INVALID_USERNAME";
      if (password !== row.password) return "INCORRECT_PASSWORD";

      await new Promise((resolve, reject) => {
        this.db.run(`DELETE FROM Teams WHERE id = ?`,
          [row.id],
          (err) => {
            if (err) return reject(err);
            resolve();
          }
        );
      });
      await new Promise((resolve, reject) => {
        this.db.run(`DELETE FROM Sessions WHERE team_id = ?`,
          [row.id],
          (err) => {
            if (err) return reject(err);
            resolve();
          }
        );
      });
      // before deleting, send message to all users that the admin has deleted all sessions, but for now 
      return "SUCCESS";
    } catch (err) {
      console.error("Database error:", err);
      return "DATABASE_ERROR";
    }
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
    this.router.post('/teams/register', this.registerTeam.bind(this));
    this.router.post('/teams/delete', this.killTeam.bind(this));
    this.router.post('/teams/update', this.updateTeam.bind(this));
    this.router.get('/teams', this.readTeams.bind(this));
  }
  async readTeams(req, res) {
    try {
      const [status, data] = await this.db_wrapper.readTeams();
      if (status === "SUCCESS") return res.status(200).json({ teams: data });
      const error_map = {
        "DATABASE_ERROR": "failed to read(unknown reason of database), try again"
      };
      return res.status(401).json({ error: error_map[status] });
    } catch (err) {
      console.error("Read error: ", err);
      return res.status(500).json({ error: "internal server error" });
    }
  }
  async updateTeam(req, res) {
    const { name, new_name, new_password, new_sessions_allowed_count } = req.body;
    if (!name) return res.status(400).json({ error: "Invalid username!(username is required for identification of unique team)" });
    try {
      const ret_val = await this.db_wrapper.updateTeam(name, new_name, new_password, new_sessions_allowed_count);
      if (ret_val === "SUCCESS") return res.status(200).json({});

      const error_map = {
        "INVALID_USERNAME": "This user is not registered",
        "DATABASE_ERROR": "failed to update(unknown reason of database), try again"
      };
      return res.status(401).json({ error: error_map[ret_val] });
    } catch (err) {
      console.error("Update error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
  async killTeam(req, res) {
    const {name, password} = req.body;
    if (!name || !password) { console.log(name); console.log(password); return res.status(400).json({ error: 'Invalid username or password' }); }
    try {
      const ret_val = await this.db_wrapper.killTeam(name, password);
      if (ret_val === "SUCCESS") return res.status(200).json({});

      const error_map = {
        "INVALID_USERNAME": "This user is not registered",
        "INCORRECT_PASSWORD": "Provide correct password",
        "DATABASE_ERROR": "failed to delete(unknown reason of database), try again"
      };
      return res.status(401).json({ error: error_map[ret_val] });
    } catch (err) {
      console.error("Deletion error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }

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
        "DATABASE_ERROR": "Failed to register(unknown reason of database), try again",
      };
      return res.status(401).json({ error: error_map[ret_val] });
    } catch (err) {
      console.error("Registration error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
}

module.exports = AdminController;

