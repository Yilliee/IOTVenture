const express = require('express');
const PlayerController = require('./controllers/PlayerController');
const { initializeDatabase } = require('./db');

const app = express();
const port = 3000;

app.use(express.json());

initializeDatabase();

// Route binding
const playerController = new PlayerController();
app.use('/api/player', playerController.router);

app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});

