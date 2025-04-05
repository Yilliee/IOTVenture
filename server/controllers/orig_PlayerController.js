const express = require('express');
const { getDatabaseConnection } = require('../db');
class DBOps_Wrapper {
  constructor() {
    this.db = getDatabaseConnection();
  }
  verifyLoginCreds(username, password) {
    let 
  }
  verifyLoginCreds(username, password) {
    let row = [];
    let ret_val = this.db.get(
      `SELECT * FROM Teams WHERE username = ?`,
      [username],
      (err, row) => {
        if (err) {
          return "INVALID_USERNAME";
        }
        console.log(row);
        row = row[0];
        console.log(row);
        if (password !== row["password"]) return "INCORRECT_PASSWORD";
        return "SUCCESS";
      }
    );
    if (ret_val !== "SUCCESS") {
      return ret_val;
    }
    const count = this.db.get(
      `SELECT COUNT(*) as count FROM Sessions WHERE team_id = ?`,
      [username],
      (err, result) => {
        if (err) {
          console.error("Unknown error in countSessions query");
          process.exit(1);
        }
        return result;
      }
    );
    if (row["sessions_allowed_count"] === count) {
      return "SESSIONS_FULL";
    }
    this.db.run(
      `INSERT INTO Sessions (user_id, session_token, expires_at, created_at) VALUES (?, ?, ?, ?)`
      [row["id"]],
      (err) => {
        if (err) {
          console.error("Error inserting session: ", err);
          process.exit(1);
        }
      }
    );
    return "SUCCESS";
  }
}

class PlayerController {
	constructor() {
    this.db_wrapper = new DPOps_Wrapper();
		this.router = express.Router();
		this.registerRoutes();
	}

	registerRoutes() {
    this.router.post('/login', this.loginUser.bind(this));
	}
  loginUser(req, res) {
		const { username, password } = req.body;
		if (!username || !password) {
			return res.status(400).json({ error: 'Both username and password are required' });
		}
    const ret_val = db_wrapper.verifyLoginCreds(username, password);
    if (ret_val === "SUCCESS") return res.status(200).json({});
    const error_map = {
      "INVALID_USERNAME": "User not found",
      "INCORRECT_PASSWORD": "Incorrect password",
      "SESSIONS_FULL": "No more sessions allowed for this team!"
    };
    return res.status(401).json({ error: error_map[ret_val] });
  }
}

module.exports = PlayerController;

