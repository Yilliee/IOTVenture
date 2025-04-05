const express = require('express');
const LogController = require('./LogController');

const app = express();
const port = 3000;

app.use(express.json());

// Route binding
const logController = new LogController();
app.use('/api/log', logController.router);

app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});

