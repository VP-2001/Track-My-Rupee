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

test("Unauthenticated user: cannot read or write users profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@example.com"
  }));
});

test("Unauthenticated user: cannot read or write expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
    userId: ALICE_UID,
    title: "Dinner",
    amount: 500,
    date: Date.now(),
    category: "Food",
    paymentMethod: "UPI"
  }));
});

test("Authenticated user: cannot read or write another user's expenses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").set({
      userId: BOB_UID,
      title: "Bob Coffee",
      amount: 150,
      date: Date.now(),
      category: "Food",
      paymentMethod: "Cash"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("bob_exp").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("expenses").doc("alice_hacked").set({
    userId: BOB_UID,
    title: "Malicious",
    amount: 9999,
    date: Date.now(),
    category: "Shopping",
    paymentMethod: "Card"
  }));
});

test("Authenticated user: can create, read, and delete their own expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const expenseRef = aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1");

  await assertSucceeds(expenseRef.set({
    userId: ALICE_UID,
    title: "Grocery Shopping",
    amount: 1250.50,
    date: Date.now(),
    category: "Groceries",
    paymentMethod: "UPI",
    notes: "Weekly fruits and milk",
    tag: "Essential"
  }));

  await assertSucceeds(expenseRef.get());
  await assertSucceeds(expenseRef.delete());
});

test("Validation: rejects expense with negative amount or missing title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const expenseRef = aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("invalid_exp");

  await assertFails(expenseRef.set({
    userId: ALICE_UID,
    title: "Invalid Negative",
    amount: -50,
    date: Date.now(),
    category: "Groceries",
    paymentMethod: "UPI"
  }));

  await assertFails(expenseRef.set({
    userId: ALICE_UID,
    title: "",
    amount: 100,
    date: Date.now(),
    category: "Groceries",
    paymentMethod: "UPI"
  }));
});
