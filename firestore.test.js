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
    royalePoints: 450,
    lifetimeRoyalePoints: 600,
    loyaltyTier: "Crimson Preferred",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validDailyLogin(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    currentStreak: 0,
    bestStreak: 0,
    totalCheckIns: 0,
    lastClaimedChips: 0,
    lastLoginAt: now,
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

test("Unauthenticated user: cannot read or write leaderboard or dailyLogins", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leaderboard").orderBy("totalXp", "desc").limit(100).get());
  await assertFails(unauthDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID)));
  await assertFails(unauthDb.collection("dailyLogins").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("dailyLogins").doc(ALICE_UID).set(validDailyLogin(ALICE_UID)));
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

test("Shadow update & schema tampering: rejects ghost fields on leaderboard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      ...validEntry(ALICE_UID),
      isAdmin: true,
    })
  );
});

test("Value poisoning: rejects negative XP, negative Royale Points, empty displayName, or invalid level", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { totalXp: -10 })));
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { royalePoints: -5 })));
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { displayName: "" })));
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID, { level: 0 })));
});

test("Immutability guard: cannot alter userId or createdAt on leaderboard update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validEntry(ALICE_UID)));

  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
    })
  );
});

test("DailyLogins: owner can create on login, update on bonus claim, and read own doc", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("dailyLogins").doc(ALICE_UID).set(validDailyLogin(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("dailyLogins").doc(ALICE_UID).get());

  // Claim daily login bonus updates streak, claimed chips, lastLoginAt, lastBonusClaimAt, updatedAt
  const claimNow = new Date(Date.now() - 500);
  await assertSucceeds(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).update({
      currentStreak: 1,
      bestStreak: 1,
      totalCheckIns: 1,
      lastClaimedChips: 1500,
      lastLoginAt: claimNow,
      lastBonusClaimAt: claimNow,
      updatedAt: claimNow,
    })
  );

  // Claim 24h daily wheel spin updates lastWheelSpinAt cleanly; future timestamp rejected
  const wheelNow = new Date(Date.now() - 250);
  await assertSucceeds(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).update({
      lastWheelSpinAt: wheelNow,
      lastLoginAt: wheelNow,
      updatedAt: wheelNow,
    })
  );
  await assertFails(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).update({
      lastWheelSpinAt: new Date(Date.now() + 60000),
      updatedAt: wheelNow,
    })
  );

  // Scoped query matching userId succeeds; unfiltered list fails
  await assertSucceeds(aliceDb.collection("dailyLogins").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("dailyLogins").get());
});

test("DailyLogins cross-user isolation & tampering guards", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("dailyLogins").doc(BOB_UID).set(validDailyLogin(BOB_UID, { currentStreak: 4 }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Cross-user get, write, update, delete fail
  await assertFails(aliceDb.collection("dailyLogins").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("dailyLogins").doc(BOB_UID).set(validDailyLogin(ALICE_UID)));
  await assertFails(aliceDb.collection("dailyLogins").doc(BOB_UID).update({ currentStreak: 99 }));
  await assertFails(aliceDb.collection("dailyLogins").doc(BOB_UID).delete());

  // Reject ghost fields, negative streak/chips, future timestamps, and immutable field mutation
  await assertFails(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).set({
      ...validDailyLogin(ALICE_UID),
      id: ALICE_UID,
    })
  );
  await assertFails(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).set(validDailyLogin(ALICE_UID, { currentStreak: -1 }))
  );
  await assertFails(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).set(
      validDailyLogin(ALICE_UID, { lastLoginAt: new Date(Date.now() + 60000) })
    )
  );

  await assertSucceeds(aliceDb.collection("dailyLogins").doc(ALICE_UID).set(validDailyLogin(ALICE_UID)));
  await assertFails(
    aliceDb.collection("dailyLogins").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
    })
  );
});

test("Default deny catch-all: rejects access to unknown collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ admin: true }));
});
