const express = require('express');

class LogController {
	constructor() {
		this.log = []; // In-memory storage
		this.router = express.Router();
		this.registerRoutes();
	}

	registerRoutes() {
		this.router.get('/', this.getLogs.bind(this));
		this.router.post('/', this.addLog.bind(this));
		this.router.put('/:id', this.updateLog.bind(this));
		this.router.delete('/:id', this.deleteLog.bind(this));
	}

	getLogs(req, res) {
		console.log(this.log);
		res.json(this.log);
	}

	addLog(req, res) {
		const { text } = req.body;
		if (!text) {
			return res.status(400).json({ error: 'Text is required' });
		}
		const newLog = { id: Date.now(), text };
		this.log.push(newLog);
		console.log(this.log);
		res.status(201).json({ success: true, log: newLog });
	}

	updateLog(req, res) {
		const { id } = req.params;
		const { text } = req.body;
		const entry = this.log.find(entry => entry.id == id);

		if (!entry) {
			return res.status(404).json({ error: 'Log not found' });
		}
		if (!text) {
			return res.status(400).json({ error: 'Text is required' });
		}

		entry.text = text;
		console.log(this.log);
		res.json({ success: true, log: entry });
	}

	deleteLog(req, res) {
		const { id } = req.params;
		const index = this.log.findIndex(entry => entry.id == id);
		if (index === -1) {
			return res.status(404).json({ error: 'Log not found' });
		}
		const deleted = this.log.splice(index, 1)[0];
		console.log(this.log);
		res.json({ success: true, deleted });
	}
}

module.exports = LogController;

