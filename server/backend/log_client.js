const axios = require('axios');
const inquirer = require('inquirer');

const BASE_URL = 'http://localhost:3000/api/log';

async function mainMenu() {
  const { action } = await inquirer.default.prompt([
    {
      type: 'list',
      name: 'action',
      message: '📘 What do you want to do?',
      choices: [
        'View all logs',
        'Create a new log',
        'Update a log',
        'Delete a log',
        'Exit',
      ],
    },
  ]);

  switch (action) {
    case 'View all logs':
      await viewLogs();
      break;
    case 'Create a new log':
      await createLog();
      break;
    case 'Update a log':
      await updateLog();
      break;
    case 'Delete a log':
      await deleteLog();
      break;
    case 'Exit':
      console.log('👋 Goodbye!');
      process.exit(0);
  }

  // Return to menu after action
  await mainMenu();
}

async function viewLogs() {
  try {
    const res = await axios.get(BASE_URL);
    if (res.data.length === 0) {
      console.log('📭 No logs yet.');
    } else {
      console.table(res.data);
    }
  } catch (err) {
    handleError(err);
  }
}

async function createLog() {
  const { text } = await inquirer.default.prompt([
    {
      type: 'input',
      name: 'text',
      message: '📝 Enter log text:',
    },
  ]);

  try {
    const res = await axios.post(BASE_URL, { text });
    console.log('✅ Created:', res.data.log);
  } catch (err) {
    handleError(err);
  }
}

async function updateLog() {
  const logs = await getLogs();
  if (logs.length === 0) return;

  const { selectedId } = await inquirer.default.prompt([
    {
      type: 'list',
      name: 'selectedId',
      message: '✏️ Choose a log to update:',
      choices: logs.map(log => ({
        name: `${log.text} (id: ${log.id})`,
        value: log.id,
      })),
    },
  ]);

  const { newText } = await inquirer.default.prompt([
    {
      type: 'input',
      name: 'newText',
      message: '🆕 Enter new text:',
    },
  ]);

  try {
    const res = await axios.put(`${BASE_URL}/${selectedId}`, { text: newText });
    console.log('✅ Updated:', res.data.log);
  } catch (err) {
    handleError(err);
  }
}

async function deleteLog() {
  const logs = await getLogs();
  if (logs.length === 0) return;

  const { selectedId } = await inquirer.default.prompt([
    {
      type: 'list',
      name: 'selectedId',
      message: '🗑️ Choose a log to delete:',
      choices: logs.map(log => ({
        name: `${log.text} (id: ${log.id})`,
        value: log.id,
      })),
    },
  ]);

  try {
    const res = await axios.delete(`${BASE_URL}/${selectedId}`);
    console.log('✅ Deleted:', res.data.deleted);
  } catch (err) {
    handleError(err);
  }
}

async function getLogs() {
  try {
    const res = await axios.get(BASE_URL);
    if (res.data.length === 0) {
      console.log('📭 No logs to choose from.');
    }
    return res.data;
  } catch (err) {
    handleError(err);
    return [];
  }
}

function handleError(err) {
  if (err.response) {
    console.error(`❌ Error ${err.response.status}:`, err.response.data);
  } else {
    console.error('❌ Error:', err.message);
  }
}

mainMenu();

