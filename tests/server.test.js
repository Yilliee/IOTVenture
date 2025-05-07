import { describe, test, beforeAll, afterAll, expect } from 'vitest';
import request from 'supertest';
import Database from 'better-sqlite3';
import { getDb, setDb, initializeDatabase } from '../src/db';
import { hash_password, verify_password } from '../src/util';

let app;

beforeAll(async () => {
  // Close existing DB and set up in-memory DB before loading app
  try { getDb().close(); } catch {};
  setDb(new Database(':memory:'));
  initializeDatabase();

  // Dynamically import app so that it uses the new DB instance
  const mod = await import('../src/app');
  app = mod.app || mod.default;});

afterAll(() => {
  // Close in-memory DB
  try { getDb().close(); } catch {};
});

// Helper for admin login
async function loginAsAdmin() {
  const res = await request(app)
    .post('/api/admin/login')
    .send({ username: 'admin', password: 'hello' });
  return res.headers['set-cookie'];
}

// Helper for team login
async function loginAsTeam() {
  const res = await request(app)
    .post('/api/team/login')
    .send({ username: 'Tech Wizards', password: 'password123' });
  return res.body.deviceToken;
}

// Password Utilities
describe('Password Utilities', () => {
  test('hash_password format', () => {
    const out = hash_password('abc');
    expect(typeof out).toBe('string');
    expect(out.split(':')).toHaveLength(2);
  });
  test('verify_password correctness', () => {
    const stored = hash_password('pw');
    expect(verify_password(stored, 'pw')).toBe(true);
    expect(verify_password(stored, 'wrong')).toBe(false);
  });
});

// Admin Auth and Routes
describe('Admin Routes', () => {
  test('login success and logout', async () => {
    const loginRes = await request(app)
      .post('/api/admin/login')
      .send({ username: 'admin', password: 'hello' });
    expect(loginRes.status).toBe(200);
    const cookie = loginRes.headers['set-cookie'];

    const logoutRes = await request(app)
      .post('/api/admin/logout')
      .set('Cookie', cookie);
    expect(logoutRes.status).toBe(200);
    expect(logoutRes.body).toEqual({ success: true });
  });

  test('protected access enforcement', async () => {
    await request(app).get('/api/admin/teams').expect(401);
  });

  test('CRUD teams flow', async () => {
    const cookie = await loginAsAdmin();

    // List teams
    const listRes = await request(app)
      .get('/api/admin/teams')
      .set('Cookie', cookie)
      .expect(200);
    expect(Array.isArray(listRes.body.teams)).toBe(true);

    // Create missing fields
    await request(app)
      .post('/api/admin/teams')
      .set('Cookie', cookie)
      .send({})
      .expect(400);

    // Create success
    const createRes = await request(app)
      .post('/api/admin/teams')
      .set('Cookie', cookie)
      .send({ name: 'Alpha', password: 'pass', maxMembers: 3 })
      .expect(201);
    expect(createRes.body).toMatchObject({ name: 'Alpha', maxMembers: 3 });
    const newId = createRes.body.id;

    // Get non-existent team
    await request(app)
      .get('/api/admin/teams/9999')
      .set('Cookie', cookie)
      .expect(404);

    // Get existing team
    const oneRes = await request(app)
      .get(`/api/admin/teams/${newId}`)
      .set('Cookie', cookie)
      .expect(200);
    expect(oneRes.body.team.id).toBe(newId);

    // Update missing
    await request(app)
      .put(`/api/admin/teams/${newId}`)
      .set('Cookie', cookie)
      .send({})
      .expect(400);

    // Update success
    await request(app)
      .put(`/api/admin/teams/${newId}`)
      .set('Cookie', cookie)
      .send({ maxMembers: 5 })
      .expect(200);

    // Delete non-existent
    await request(app)
      .delete('/api/admin/teams/9999')
      .set('Cookie', cookie)
      .expect(404);

    // Delete success
    await request(app)
      .delete(`/api/admin/teams/${newId}`)
      .set('Cookie', cookie)
      .expect(204);
  });

  test('Force submit user', async () => {
    const cookie = await loginAsAdmin();
    await request(app)
      .post('/api/admin/users/1/force-submit')
      .set('Cookie', cookie)
      .expect(200)
      .expect(res => expect(res.body.success).toBeDefined());
  });

  test('Send and deliver messages', async () => {
    const cookie = await loginAsAdmin();
    const sendRes = await request(app)
      .post('/api/admin/send-message')
      .set('Cookie', cookie)
      .send({ teamId: 1, content: 'Hello Team' })
      .expect(200);
    expect(sendRes.body).toHaveProperty('messageId');
  });
});

// Team Login
describe('Team Login', () => {
  test('invalid creds', async () => {
    await request(app)
      .post('/api/team/login')
      .send({ device_name: 'dev', username: 'X', password: 'Y' })
      .expect(401, { error: 'WRONG_CREDS' });
  });

  test('success', async () => {
    const res = await request(app)
      .post('/api/team/login')
      .send({ device_name: 'dev', username: 'Tech Wizards', password: 'password123' })
      .expect(200);
    expect(res.body).toHaveProperty('deviceToken');
    expect(Array.isArray(res.body.challenges)).toBe(true);
  });
});

// Update Leaderboard
describe('Update Leaderboard', () => {
  test('invalid token', async () => {
    await request(app)
      .post('/api/update-leaderboard')
      .send({ deviceToken: 'bad', solves: [], isFinalSubmission: false })
      .expect(401);
  });

  test('valid submission', async () => {
    const token = await loginAsTeam();
    const res = await request(app)
      .post('/api/update-leaderboard')
      .send({ deviceToken: token, solves: [{ challengeId: 1 }], isFinalSubmission: false })
      .expect(200);
    expect(res.body.success).toBe(true);
  });
});

// Get Messages
describe('Get Messages', () => {
  test('invalid token', async () => {
    await request(app)
      .get('/api/messages')
      .query({ deviceToken: 'nope' })
      .expect(401);
  });

  test('retrieve and mark delivered', async () => {
    const token = await loginAsTeam();
    const cookie = await loginAsAdmin();
    await request(app)
      .get('/api/admin/send-message')
      .set('Cookie', cookie)
      .send({ teamId: 1, content: 'Msg' });

    const res = await request(app)
      .get('/api/messages')
      .query({ deviceToken: token })
      .expect(200);
    expect(Array.isArray(res.body.messages)).toBe(true);
  });
});

// Public Leaderboard
describe('Public Leaderboard', () => {
  test('structure and competitionEnded flag', async () => {
    const res = await request(app)
      .get('/api/leaderboard')
      .expect(200);
    expect(Array.isArray(res.body.challenges)).toBe(true);
    expect(Array.isArray(res.body.teamSolves)).toBe(true);
    expect(typeof res.body.competitionEnded).toBe('boolean');
  });
});

// Admin Challenges CRUD
describe('Admin Challenges', () => {
  let cookie;
  beforeAll(async () => { cookie = await loginAsAdmin(); });

  test('List challenges', async () => {
    const res = await request(app)
      .get('/api/admin/challenges')
      .set('Cookie', cookie)
      .expect(200);
    expect(Array.isArray(res.body.challenges)).toBe(true);
  });

  test('Get one not found', async () => {
    await request(app)
      .get('/api/admin/challenges/9999')
      .set('Cookie', cookie)
      .expect(404);
  });

  test('Get one success', async () => {
    const res = await request(app)
      .get('/api/admin/challenges/1')
      .set('Cookie', cookie)
      .expect(200);
    expect(res.body).toHaveProperty('id');
  });

  test('Create missing fields', async () => {
    await request(app)
      .post('/api/admin/challenges')
      .set('Cookie', cookie)
      .send({})
      .expect(400);
  });

  test('Create success', async () => {
    const payload = { name: 'TestChal', shortName: 'TC', points: 50,
      location: { topLeft: { lat: 0, lng: 0 }, bottomRight: { lat: 1, lng: 1 } },
      keyHash: 'xyz'
    };
    const res = await request(app)
      .post('/api/admin/challenges')
      .set('Cookie', cookie)
      .send(payload)
      .expect(201);
    expect(res.body.name).toBe('TestChal');
  });

  test('Update no fields', async () => {
    await request(app)
      .put('/api/admin/challenges/1')
      .set('Cookie', cookie)
      .send({})
      .expect(400);
  });

  test('Update not found', async () => {
    await request(app)
      .put('/api/admin/challenges/9999')
      .set('Cookie', cookie)
      .send({ name: 'X' })
      .expect(404);
  });

  test('Update success', async () => {
    await request(app)
      .put('/api/admin/challenges/1')
      .set('Cookie', cookie)
      .send({ points: 123 })
      .expect(200);
  });

  test('Delete not found', async () => {
    await request(app)
      .delete('/api/admin/challenges/9999')
      .set('Cookie', cookie)
      .expect(404);
  });

  test('Delete success', async () => {
    const newRes = await request(app)
      .post('/api/admin/challenges')
      .set('Cookie', cookie)
      .send({ name: 'ToDel', shortName: 'TD', points: 1,
        location: { topLeft: { lat:0, lng:0 }, bottomRight: { lat:1, lng:1 } },
        keyHash: 'abc'
      });
    const delId = newRes.body.id;
    await request(app)
      .delete(`/api/admin/challenges/${delId}`)
      .set('Cookie', cookie)
      .expect(204);
  });
});
