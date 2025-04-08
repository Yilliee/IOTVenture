const express = require('express');
const PlayerController = require('./controllers/PlayerController');
const AdminController = require('./controllers/AdminController');
const { initializeDatabase } = require('./db');

const app = express();
const port = 3000;

app.use(express.json());

initializeDatabase();

// Route binding
const playerController = new PlayerController();
app.use('/api/player', playerController.router);
const adminController = new AdminController();
app.use('/api/admin', adminController.router);

app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});

