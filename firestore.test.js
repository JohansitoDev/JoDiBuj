const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 5000);
}

function validUserPayload(uid, username = "Artista1") {
  const now = pastTimestamp();
  return {
    userId: uid,
    username,
    displayName: "Ana Dibujos",
    email: `${uid}@example.com`,
    bio: "Amo dibujar!",
    avatarIcon: "brush",
    avatarColor: "#1CB0F6",
    totalPoints: 120,
    level: 2,
    gamesPlayed: 3,
    wins: 1,
    currentStreak: 2,
    createdAt: now,
    updatedAt: now,
  };
}

function validRoomPayload(roomCode, hostId) {
  const now = pastTimestamp();
  return {
    roomCode,
    hostId,
    hostName: "Artista1",
    status: "waiting",
    promptWord: "Pez",
    promptKey: "fish",
    currentRound: 1,
    totalRounds: 3,
    maxPlayers: 6,
    roundDurationSec: 60,
    playerUids: [hostId],
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write users, leaderboard, or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("totalPoints", ">=", 0).get());
  await assertFails(unauthDb.collection("rooms").where("status", "==", "waiting").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
});

test("User profile: owner can create/read/update, other user cannot read PII", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPoints: 250,
      level: 3,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Leaderboard: owner can write, signed-in users can query with totalPoints >= 0", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceArt",
      avatarIcon: "brush",
      avatarColor: "#58CC02",
      totalPoints: 300,
      level: 4,
      wins: 2,
      updatedAt: now,
    })
  );

  // Bob cannot overwrite Alice's leaderboard entry
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Hacked",
      avatarIcon: "brush",
      avatarColor: "#58CC02",
      totalPoints: 9999,
      level: 99,
      wins: 99,
      updatedAt: now,
    })
  );

  // Bob can query global leaderboard with matching filter
  await assertSucceeds(
    bobDb.collection("leaderboard").where("totalPoints", ">=", 0).orderBy("totalPoints", "desc").limit(50).get()
  );
});

test("Room lifecycle: create, join up to 6 players, draw, and peer vote (no self-vote)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const roomId = "JD48K2";
  const now = pastTimestamp();

  // Alice creates room
  await assertSucceeds(aliceDb.collection("rooms").doc(roomId).set(validRoomPayload(roomId, ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("players").doc(ALICE_UID).set({
      userId: ALICE_UID,
      roomId,
      username: "Alice",
      avatarIcon: "cat",
      avatarColor: "#1CB0F6",
      hasSubmittedRound: 0,
      joinedAt: now,
      updatedAt: now,
    })
  );

  // Bob queries waiting rooms and joins
  await assertSucceeds(bobDb.collection("rooms").where("status", "==", "waiting").get());
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).update({
      playerUids: [ALICE_UID, BOB_UID],
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("players").doc(BOB_UID).set({
      userId: BOB_UID,
      roomId,
      username: "Bob",
      avatarIcon: "fish",
      avatarColor: "#58CC02",
      hasSubmittedRound: 0,
      joinedAt: now,
      updatedAt: now,
    })
  );

  // Non-host Bob cannot change room status
  await assertFails(
    bobDb.collection("rooms").doc(roomId).update({
      status: "drawing",
      updatedAt: pastTimestamp(),
    })
  );

  // Host Alice starts drawing
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).update({
      status: "drawing",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice and Bob submit drawings
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("drawings").doc(`1_${ALICE_UID}`).set({
      userId: ALICE_UID,
      roomId,
      username: "Alice",
      avatarColor: "#1CB0F6",
      round: 1,
      promptWord: "Pez",
      strokes: [{ colorHex: "#1CB0F6", strokeWidth: 8, points: [0.1, 0.2, 0.5, 0.6] }],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Host transitions to voting
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).update({
      status: "voting",
      updatedAt: pastTimestamp(),
    })
  );

  // Self-voting MUST fail
  await assertFails(
    aliceDb.collection("rooms").doc(roomId).collection("votes").doc(`1_${ALICE_UID}`).set({
      voterId: ALICE_UID,
      targetUserId: ALICE_UID,
      roomId,
      round: 1,
      points: 50,
      createdAt: pastTimestamp(),
    })
  );

  // Voting for friend succeeds
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("votes").doc(`1_${ALICE_UID}`).set({
      voterId: ALICE_UID,
      targetUserId: BOB_UID,
      roomId,
      round: 1,
      points: 50,
      createdAt: pastTimestamp(),
    })
  );

  // Non-member Charlie cannot vote in room
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("rooms").doc(roomId).collection("votes").doc(`1_${CHARLIE_UID}`).set({
      voterId: CHARLIE_UID,
      targetUserId: ALICE_UID,
      roomId,
      round: 1,
      points: 50,
      createdAt: pastTimestamp(),
    })
  );
});
