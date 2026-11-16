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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write game_votes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("game_votes").doc("genshin_impact").get());
  await assertFails(
    unauthDb
      .collection("game_votes")
      .doc("genshin_impact")
      .collection("votes")
      .doc(ALICE_UID)
      .get()
  );
  await assertFails(
    unauthDb.collection("game_votes").doc("genshin_impact").set({
      gameId: "genshin_impact",
      runsGreatCount: 1,
      lagsCount: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user cannot list entire game_votes collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("game_votes").get());
});

test("Authenticated user can atomically create summary and personal vote", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const batch = aliceDb.batch();
  const summaryRef = aliceDb.collection("game_votes").doc("genshin_impact");
  const voteRef = summaryRef.collection("votes").doc(ALICE_UID);

  batch.set(summaryRef, {
    gameId: "genshin_impact",
    runsGreatCount: 1,
    lagsCount: 0,
    updatedAt: now,
  });
  batch.set(voteRef, {
    userId: ALICE_UID,
    gameId: "genshin_impact",
    verdict: "runs_great",
    deviceSoc: "Snapdragon 8 Gen 3",
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(batch.commit());
  await assertSucceeds(summaryRef.get());
  await assertSucceeds(voteRef.get());
});

test("Authenticated user can update vote from runs_great to lags atomically", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const t1 = new Date(Date.now() - 5000);
  const summaryRef = aliceDb.collection("game_votes").doc("pubg_mobile");
  const voteRef = summaryRef.collection("votes").doc(ALICE_UID);

  const createBatch = aliceDb.batch();
  createBatch.set(summaryRef, {
    gameId: "pubg_mobile",
    runsGreatCount: 1,
    lagsCount: 0,
    updatedAt: t1,
  });
  createBatch.set(voteRef, {
    userId: ALICE_UID,
    gameId: "pubg_mobile",
    verdict: "runs_great",
    deviceSoc: "Snapdragon 8 Gen 3",
    createdAt: t1,
    updatedAt: t1,
  });
  await assertSucceeds(createBatch.commit());

  const t2 = new Date(Date.now() - 1000);
  const updateBatch = aliceDb.batch();
  updateBatch.update(summaryRef, {
    runsGreatCount: 0,
    lagsCount: 1,
    updatedAt: t2,
  });
  updateBatch.update(voteRef, {
    verdict: "lags",
    updatedAt: t2,
  });
  await assertSucceeds(updateBatch.commit());
});

test("Cross-user access to another user's vote document is denied", async () => {
  const t1 = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("game_votes").doc("genshin_impact").set({
      gameId: "genshin_impact",
      runsGreatCount: 1,
      lagsCount: 0,
      updatedAt: t1,
    });
    await adminDb
      .collection("game_votes")
      .doc("genshin_impact")
      .collection("votes")
      .doc(BOB_UID)
      .set({
        userId: BOB_UID,
        gameId: "genshin_impact",
        verdict: "runs_great",
        deviceSoc: "Tensor G4",
        createdAt: t1,
        updatedAt: t1,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("game_votes")
      .doc("genshin_impact")
      .collection("votes")
      .doc(BOB_UID)
      .get()
  );
  await assertFails(
    aliceDb
      .collection("game_votes")
      .doc("genshin_impact")
      .collection("votes")
      .doc(BOB_UID)
      .update({ verdict: "lags", updatedAt: new Date(Date.now() - 500) })
  );
});

test("Shadow update with ghost field or standalone summary inflation is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Standalone summary write without user vote document fails
  await assertFails(
    aliceDb.collection("game_votes").doc("minecraft").set({
      gameId: "minecraft",
      runsGreatCount: 1,
      lagsCount: 0,
      updatedAt: now,
    })
  );

  // Ghost field in vote document fails
  const batch = aliceDb.batch();
  const summaryRef = aliceDb.collection("game_votes").doc("minecraft");
  const voteRef = summaryRef.collection("votes").doc(ALICE_UID);
  batch.set(summaryRef, {
    gameId: "minecraft",
    runsGreatCount: 1,
    lagsCount: 0,
    updatedAt: now,
  });
  batch.set(voteRef, {
    userId: ALICE_UID,
    gameId: "minecraft",
    verdict: "runs_great",
    deviceSoc: "Snapdragon 8 Gen 3",
    isAdmin: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertFails(batch.commit());
});
