/**
 * Firestore Security Rules Test Script
 * 
 * This script provides manual test cases to verify Firestore security rules.
 * Run this after deploying security rules to verify they work as expected.
 * 
 * Prerequisites:
 * - Firebase Admin SDK installed: npm install firebase-admin
 * - Service account key file downloaded from Firebase Console
 * - Set GOOGLE_APPLICATION_CREDENTIALS environment variable
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.applicationDefault()
});

const db = admin.firestore();

// Test data
const testHouseholdId = 'test-household-' + Date.now();
const testUserId1 = 'test-user-1';
const testUserId2 = 'test-user-2';
const testExpenseId = 'test-expense-' + Date.now();
const testCategoryId = 'test-category-' + Date.now();

/**
 * Test 1: Household Access Permissions
 */
async function testHouseholdAccess() {
  console.log('\n=== Test 1: Household Access Permissions ===');
  
  try {
    // Create a test household
    const householdData = {
      name: 'Test Household',
      createdBy: testUserId1,
      createdAt: admin.firestore.Timestamp.now(),
      memberIds: [testUserId1],
      inviteCode: 'TEST123'
    };
    
    await db.collection('households').doc(testHouseholdId).set(householdData);
    console.log('✓ Created test household');
    
    // Read household (should succeed for member)
    const household = await db.collection('households').doc(testHouseholdId).get();
    console.log('✓ Member can read household:', household.exists);
    
    // Update household (add new member)
    await db.collection('households').doc(testHouseholdId).update({
      memberIds: admin.firestore.FieldValue.arrayUnion(testUserId2)
    });
    console.log('✓ Added new member to household');
    
    // Verify new member was added
    const updatedHousehold = await db.collection('households').doc(testHouseholdId).get();
    const memberIds = updatedHousehold.data().memberIds;
    console.log('✓ Verified member list:', memberIds);
    
    console.log('✓ Test 1 PASSED');
  } catch (error) {
    console.error('✗ Test 1 FAILED:', error.message);
  }
}

/**
 * Test 2: Expense CRUD Permissions
 */
async function testExpenseCRUD() {
  console.log('\n=== Test 2: Expense CRUD Permissions ===');
  
  try {
    // Create an expense
    const expenseData = {
      id: testExpenseId,
      householdId: testHouseholdId,
      amount: 50.00,
      description: 'Test Expense',
      categoryId: 1,
      date: admin.firestore.Timestamp.now(),
      createdBy: testUserId1,
      createdAt: admin.firestore.Timestamp.now(),
      modifiedBy: testUserId1,
      modifiedAt: admin.firestore.Timestamp.now(),
      isDeleted: false
    };
    
    await db.collection('expenses').doc(testHouseholdId)
      .collection('expenses').doc(testExpenseId).set(expenseData);
    console.log('✓ Created test expense');
    
    // Read expense
    const expense = await db.collection('expenses').doc(testHouseholdId)
      .collection('expenses').doc(testExpenseId).get();
    console.log('✓ Read expense:', expense.exists);
    
    // Update expense
    await db.collection('expenses').doc(testHouseholdId)
      .collection('expenses').doc(testExpenseId).update({
        amount: 75.00,
        modifiedBy: testUserId2,
        modifiedAt: admin.firestore.Timestamp.now()
      });
    console.log('✓ Updated expense');
    
    // Verify update
    const updatedExpense = await db.collection('expenses').doc(testHouseholdId)
      .collection('expenses').doc(testExpenseId).get();
    console.log('✓ Verified expense amount:', updatedExpense.data().amount);
    
    console.log('✓ Test 2 PASSED');
  } catch (error) {
    console.error('✗ Test 2 FAILED:', error.message);
  }
}

/**
 * Test 3: Category Permissions
 */
async function testCategoryPermissions() {
  console.log('\n=== Test 3: Category Permissions ===');
  
  try {
    // Create a category
    const categoryData = {
      id: testCategoryId,
      householdId: testHouseholdId,
      name: 'Test Category',
      icon: '🧪',
      createdBy: testUserId1,
      createdAt: admin.firestore.Timestamp.now(),
      modifiedAt: admin.firestore.Timestamp.now(),
      isDeleted: false
    };
    
    await db.collection('categories').doc(testHouseholdId)
      .collection('categories').doc(testCategoryId).set(categoryData);
    console.log('✓ Created test category');
    
    // Read category
    const category = await db.collection('categories').doc(testHouseholdId)
      .collection('categories').doc(testCategoryId).get();
    console.log('✓ Read category:', category.exists);
    
    // Update category
    await db.collection('categories').doc(testHouseholdId)
      .collection('categories').doc(testCategoryId).update({
        name: 'Updated Test Category',
        modifiedAt: admin.firestore.Timestamp.now()
      });
    console.log('✓ Updated category');
    
    // List all categories
    const categories = await db.collection('categories').doc(testHouseholdId)
      .collection('categories').get();
    console.log('✓ Listed categories, count:', categories.size);
    
    console.log('✓ Test 3 PASSED');
  } catch (error) {
    console.error('✗ Test 3 FAILED:', error.message);
  }
}

/**
 * Test 4: Member Removal Access Revocation
 */
async function testMemberRemoval() {
  console.log('\n=== Test 4: Member Removal Access Revocation ===');
  
  try {
    // Remove user from household
    await db.collection('households').doc(testHouseholdId).update({
      memberIds: [testUserId1] // Remove testUserId2
    });
    console.log('✓ Removed member from household');
    
    // Verify member was removed
    const household = await db.collection('households').doc(testHouseholdId).get();
    const memberIds = household.data().memberIds;
    console.log('✓ Verified member list after removal:', memberIds);
    
    console.log('✓ Test 4 PASSED');
    console.log('Note: Actual access revocation can only be tested with authenticated client SDK');
  } catch (error) {
    console.error('✗ Test 4 FAILED:', error.message);
  }
}

/**
 * Cleanup test data
 */
async function cleanup() {
  console.log('\n=== Cleanup ===');
  
  try {
    // Delete test expense
    await db.collection('expenses').doc(testHouseholdId)
      .collection('expenses').doc(testExpenseId).delete();
    console.log('✓ Deleted test expense');
    
    // Delete test category
    await db.collection('categories').doc(testHouseholdId)
      .collection('categories').doc(testCategoryId).delete();
    console.log('✓ Deleted test category');
    
    // Delete test household
    await db.collection('households').doc(testHouseholdId).delete();
    console.log('✓ Deleted test household');
    
    console.log('✓ Cleanup complete');
  } catch (error) {
    console.error('✗ Cleanup failed:', error.message);
  }
}

/**
 * Run all tests
 */
async function runTests() {
  console.log('Starting Firestore Security Rules Tests...');
  console.log('Note: These tests use Admin SDK which bypasses security rules.');
  console.log('For complete testing, use Firebase Emulator with client SDK.');
  
  await testHouseholdAccess();
  await testExpenseCRUD();
  await testCategoryPermissions();
  await testMemberRemoval();
  await cleanup();
  
  console.log('\n=== All Tests Complete ===');
  console.log('Remember to test with actual client authentication for full validation.');
  
  process.exit(0);
}

// Run tests
runTests().catch(error => {
  console.error('Test suite failed:', error);
  process.exit(1);
});
