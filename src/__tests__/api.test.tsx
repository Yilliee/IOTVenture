import { getChallenges } from '../lib/api';

beforeEach(() => {
  fetchMock.resetMocks();
});

test('returns challenge list on 200 response', async () => {
  const mockData = {
    challenges: [{ id: "1", name: "Challenge 1", shortName: "C1", points: 100, keyHash: "xyz",
      location: {
        topLeft: { lat: 0, lng: 0 },
        bottomRight: { lat: 1, lng: 1 }
      }
    }]
  };

  fetchMock.mockResponseOnce(JSON.stringify(mockData), { status: 200 });

  const { status, challenges, error } = await getChallenges();

  expect(status).toBe(200);
  expect(challenges).toEqual(mockData.challenges);
  expect(error).toBeUndefined();
});

test('returns challenge from list on 200 response', async () => {
  const mockData = {
    challenges: [{ id: "1", name: "Challenge 1", shortName: "C1", points: 100, keyHash: "xyz",
      location: {
        topLeft: { lat: 0, lng: 0 },
        bottomRight: { lat: 1, lng: 1 }
      }
    }, { id: "2", name: "Challenge 2", shortName: "C2", points: 105, keyHash: "ayz",
      location: {
        topLeft: { lat: 2, lng: 3 },
        bottomRight: { lat: 9, lng: 8 }
      }
    }]
  };

  fetchMock.mockResponseOnce(JSON.stringify(mockData), { status: 200 });

  const { status, challenges, error } = await getChallenge("2");

  expect(status).toBe(200);
  expect(challenge).toEqual(mockData.challenges[1]);
  expect(error).toBeUndefined();
});

test('returns error due to invalid challenge id', async () => {
  const mockData = {
    challenges: [{ id: "1", name: "Challenge 1", shortName: "C1", points: 100, keyHash: "xyz",
      location: {
        topLeft: { lat: 0, lng: 0 },
        bottomRight: { lat: 1, lng: 1 }
      }
    }, { id: "2", name: "Challenge 2", shortName: "C2", points: 105, keyHash: "ayz",
      location: {
        topLeft: { lat: 2, lng: 3 },
        bottomRight: { lat: 9, lng: 8 }
      }
    }]
  };

  fetchMock.mockResponseOnce(JSON.stringify(mockData), { status: 404, error: "the error is from the test!" });

  const { status, challenges, error } = await getChallenge("3");

  expect(status).toBe(404);
  expect(challenge).toBeUndefined();
  expect(error).toBe("the error is from the test!");
});

import { createChallenge } from "../lib/api";

test("createChallenge posts challenge data and returns the created challenge", async () => {
  const mockChallenge = {
    id: 101,
    title: "New Challenge",
    category: "Web",
    points: 100
  }

  fetchMock.mockResponseOnce(JSON.stringify(mockChallenge))

  const response = await createChallenge({
    title: "New Challenge",
    category: "Web",
    points: 100,
    description: "Find the flag.",
    flag: "flag{test}"
  })

  expect(fetchMock).toHaveBeenCalledWith(
    expect.stringContaining("/admin/challenges"),
    expect.objectContaining({
      method: "POST",
      body: JSON.stringify({
        title: "New Challenge",
        category: "Web",
        points: 100,
        description: "Find the flag.",
        flag: "flag{test}"
      })
    })
  )
  expect(response.status).toBe(200)
  expect(response.challenge).toEqual(mockChallenge)
})
import { updateChallenge } from "../lib/api";

test("updateChallenge sends updated challenge data", async () => {
  const updatedChallenge = {
    id: 101,
    title: "Updated Challenge",
    category: "Pwn",
    points: 200
  }

  fetchMock.mockResponseOnce(JSON.stringify(updatedChallenge))

  const response = await updateChallenge(101, {
    title: "Updated Challenge",
    category: "Pwn",
    points: 200,
    description: "New description",
    flag: "flag{updated}"
  })

  expect(fetchMock).toHaveBeenCalledWith(
    expect.stringContaining("/admin/challenges/101"),
    expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({
        title: "Updated Challenge",
        category: "Pwn",
        points: 200,
        description: "New description",
        flag: "flag{updated}"
      })
    })
  )

  expect(response.status).toBe(200)
  expect(response.challenge).toEqual(updatedChallenge)
})
import { deleteChallenge } from "../lib/api"

test("deleteChallenge deletes a challenge by ID", async () => {
  fetchMock.mockResponseOnce(JSON.stringify({ success: true }))

  const response = await deleteChallenge(101)

  expect(fetchMock).toHaveBeenCalledWith(
    expect.stringContaining("/admin/challenges/101"),
    expect.objectContaining({ method: "DELETE" })
  )
  expect(response.status).toBe(200)
  expect(response.success).toBe(true)
})

