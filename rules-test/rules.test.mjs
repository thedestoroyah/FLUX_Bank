// Test Security Rules trên Firestore emulator (không cần đăng nhập Firebase). Chạy: xem README mục "Test rules".
import { test, before, after, beforeEach } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc, writeBatch, getDoc } from 'firebase/firestore';

let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-flux', firestore: { rules: readFileSync('../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8085 } });
});
after(async () => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/off'), { uid: 'off', role: 'BANK_OFFICER' });
    await setDoc(doc(db, 'users/cust'), { uid: 'cust', role: 'CUSTOMER' });
    await setDoc(doc(db, 'accounts/acc1'), { ownerUid: 'cust', type: 'CHECKING', balance: 100000, accountNumber: '886800000001' });
    await setDoc(doc(db, 'accounts/other'), { ownerUid: 'x', type: 'CHECKING', balance: 100000 });
    await setDoc(doc(db, 'accounts/mort'), { ownerUid: 'cust', type: 'MORTGAGE', balance: 1000000, loanAmount: 1000000, termMonths: 12 });
    await setDoc(doc(db, 'transactions/old'), { ownerUid: 'cust', accountId: 'acc1', type: 'DEPOSIT', amount: 10000, balanceAfter: 100000 });
  });
});
const cust = () => env.authenticatedContext('cust').firestore();
const off = () => env.authenticatedContext('off').firestore();
const tx = (id, type, amount, balanceAfter, accountId = 'acc1') => ({ id, ownerUid: 'cust', accountId, type, amount, balanceAfter, accountNumber: '886800000001', description: '', createdAt: 1 });
function move(db, accountId, txId, type, amount, newBalance, txOverride = {}) {
  const b = writeBatch(db);
  b.update(doc(db, `accounts/${accountId}`), { balance: newBalance, lastTxId: txId });
  b.set(doc(db, `transactions/${txId}`), { ...tx(txId, type, amount, newBalance, accountId), ...txOverride });
  return b.commit();
}

test('valid deposit batch allowed', () => assertSucceeds(move(cust(), 'acc1', 't1', 'DEPOSIT', 50000, 150000)));
test('valid withdraw to zero allowed', () => assertSucceeds(move(cust(), 'acc1', 't1', 'WITHDRAW', 100000, 0)));
test('balance-only update denied', () => assertFails(updateDoc(doc(cust(), 'accounts/acc1'), { balance: 999999999 })));
test('tx alone (lastTxId not pointing to it) denied', () => assertFails(setDoc(doc(cust(), 'transactions/t9'), tx('t9', 'DEPOSIT', 50000, 150000))));
test('withdraw to -1 denied', () => assertFails(move(cust(), 'acc1', 't1', 'WITHDRAW', 100001, -1)));
test('deposit with wrong arithmetic denied', () => assertFails(move(cust(), 'acc1', 't1', 'DEPOSIT', 50000, 250000, { balanceAfter: 250000 })));
test('deposit over 100M denied', () => assertFails(move(cust(), 'acc1', 't1', 'DEPOSIT', 100000001, 100100001)));
test('deposit under 10k denied', () => assertFails(move(cust(), 'acc1', 't1', 'DEPOSIT', 9999, 109999)));
test('reusing existing tx id denied', () => assertFails(move(cust(), 'acc1', 'old', 'DEPOSIT', 50000, 150000)));
test('moving another customers account denied', () => assertFails(move(cust(), 'other', 't1', 'DEPOSIT', 50000, 150000)));
test('money move on mortgage denied', () => assertFails(move(cust(), 'mort', 't1', 'DEPOSIT', 50000, 1050000)));
test('officer cannot update balance', () => assertFails(updateDoc(doc(off(), 'accounts/acc1'), { balance: 1 })));
test('customer reads own transaction', () => assertSucceeds(getDoc(doc(cust(), 'transactions/old'))));
test('customer writes rates denied', () => assertFails(setDoc(doc(cust(), 'config/rates'), { savingRate: 5, mortgageRate: 8.5 })));
test('officer writes valid rates', () => assertSucceeds(setDoc(doc(off(), 'config/rates'), { savingRate: 5, mortgageRate: 8.5, updatedAt: 1, updatedBy: 'off', updatedByName: 'A' })));
test('officer writes rate above 30 denied', () => assertFails(setDoc(doc(off(), 'config/rates'), { savingRate: 31, mortgageRate: 8.5 })));
test('customer reads rates', () => assertSucceeds(getDoc(doc(cust(), 'config/rates'))));
test('officer opens valid mortgage', () => assertSucceeds(setDoc(doc(off(), 'accounts/m2'), { ownerUid: 'cust', type: 'MORTGAGE', balance: 5000000, loanAmount: 5000000, termMonths: 240, annualRate: 8.5 })));
test('officer mortgage term 0 denied', () => assertFails(setDoc(doc(off(), 'accounts/m3'), { ownerUid: 'cust', type: 'MORTGAGE', balance: 5000000, loanAmount: 5000000, termMonths: 0 })));
test('customer cannot open accounts', () => assertFails(setDoc(doc(cust(), 'accounts/s1'), { ownerUid: 'cust', type: 'SAVING', balance: 0 })));
