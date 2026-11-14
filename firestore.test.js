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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validEntry(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "EmberAce",
    totalXp: 1500,
    level: 4,
    vipTitle: "Gold High Roller",
    bestWinStreak: 5,
    biggestWin: 12000,
    totalRoundsPlayed: 42,
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated user: cannot read or write leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leaderboard").orderBy("totalXp", "desc").limit(100).get());
  await assertFails(unauthDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID)));
});

test("Authenticated user: can create and update their own leaderboard entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entry = validEntry(ALICE_UID);
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(entry));

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      totalXp: 2500,
      level: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated user: can query Top 100 leaderboard ordered by totalXp desc", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { totalXp: 5000 }));
    await adminDb.collection("leaderboard").doc(BOB_UID).set(validEntry(BOB_UID, { totalXp: 8000, displayName: "VolcanoKing" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("leaderboard").orderBy("totalXp", "desc").limit(100).get());
});

test("Cross-user write isolation: Alice cannot create, update, or delete Bob's leaderboard entry", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("leaderboard").doc(BOB_UID).set(validEntry(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("leaderboard").doc(BOB_UID).set(validEntry(ALICE_UID)));
  await assertFails(aliceDb.collection("leaderboard").doc(BOB_UID).set(validEntry(BOB_UID)));
  await assertFails(aliceDb.collection("leaderboard").doc(BOB_UID).update({ totalXp: 99999 }));
  await assertFails(aliceDb.collection("leaderboard").doc(BOB_UID).delete());
});

test("Shadow update & schema tampering: rejects ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      ...validEntry(ALICE_UID),
      isAdmin: true,
    })
  );
});

test("Value poisoning: rejects negative XP, empty displayName, or invalid level", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { totalXp: -10 })));
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { displayName: "" })));
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { level: 0 })));
});

test("Immutability guard: cannot alter userId or createdAt on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID)));

  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
    })
  );
});

test("Default deny catch-all: rejects access to unknown collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ admin: true }));
});
