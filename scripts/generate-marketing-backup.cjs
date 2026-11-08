#!/usr/bin/env node
// Deterministic fictional data only. Run: node scripts/generate-marketing-backup.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { spawnSync } = require('child_process');
const root = path.resolve(__dirname, '..');
const out = path.join(root, 'artifacts/fixtures/marketing-2026-10');
fs.mkdirSync(out, { recursive: true });
const id = (type, n) => `${type}0000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
const stamp = (day, time = '12:00:00') => Date.parse(`${day}T${time}Z`);
const created = stamp('2026-09-01');
const accountNames = ['Cash', 'Everyday bank', 'Savings', 'Emergency fund', 'Holiday fund', 'Investments'];
const accounts = accountNames.map((name, i) => ({ id: id(1, i + 1), name, currency: 'AED', color: [-15414114,-10396690,-7057240,-12627531,-459786,-11746305][i], icon: ['cash','bank','bank','shield','travel','leaf'][i], orderNum: i, includeInBalance: i !== 5 }));
const categoryNames = ['Salary', 'Freelance', 'Groceries', 'Eating out', 'Transport', 'Home & bills', 'Shopping', 'Health', 'Entertainment', 'Loans'];
const categories = categoryNames.map((name, i) => ({ id: id(2, i + 1), name, color: [-15414114,-5573920,-15414114,-459786,-2151,-49056,-9744897,-2765569,-876752,-10396690][i], orderNum: i }));
const transactions = [];
function transaction(day, account, type, amount, title, category = null, extra = {}) {
  const row = { id: id(8, transactions.length + 1), accountId: accounts[account].id, type, amount, title,
    dateTime: stamp(day), categoryId: category == null ? null : categories[category].id, ...extra };
  transactions.push(row); return row;
}
[800, 8000, 16000, 10000, 3000, 25000].forEach((amount, account) => transaction('2026-09-01', account, 'INCOME', amount, 'Adjust balance', null, { dateTime: stamp('2026-09-01', '06:00:00'), description: 'Fictional opening balance, not earned income.' }));
for (const month of ['09', '10']) {
  transaction(`2026-${month}-01`, 1, 'INCOME', 15000, 'Monthly salary', 0);
  transaction(`2026-${month}-02`, 1, 'EXPENSE', 4500, 'Apartment rent', 5);
  transaction(`2026-${month}-05`, 1, 'EXPENSE', 349, 'Home internet', 5);
  transaction(`2026-${month}-07`, 1, 'EXPENSE', 220, 'Gym membership', 7);
  transaction(`2026-${month}-14`, 1, 'EXPENSE', 49, 'Movie subscription', 8);
  for (const [day, amount] of [[3, 320.5], [10, 285.75], [17, 410.25], [24, 265]]) transaction(`2026-${month}-${String(day).padStart(2, '0')}`, 1, 'EXPENSE', amount, 'Weekly groceries', 2);
  for (const day of [4, 8, 12, 16, 20, 23]) {
    transaction(`2026-${month}-${String(day).padStart(2, '0')}`, 1, 'EXPENSE', 42 + day, 'Lunch with friends', 3);
    transaction(`2026-${month}-${String(day).padStart(2, '0')}`, 1, 'EXPENSE', 15 + day, 'Metro and taxi', 4);
  }
  transaction(`2026-${month}-15`, 1, 'TRANSFER', 2000, 'Move money to savings', null, { toAccountId: accounts[2].id, toAmount: 2000 });
  transaction(`2026-${month}-18`, 1, 'TRANSFER', 500, 'Set aside for holiday', null, { toAccountId: accounts[4].id, toAmount: 500 });
}
transaction('2026-10-20', 1, 'INCOME', 1800, 'Freelance design project', 1);
transaction('2026-10-21', 1, 'EXPENSE', 420, 'Running shoes', 6);
transaction('2026-10-22', 0, 'EXPENSE', 35, 'Coffee and a sandwich', 3);
transaction('2026-10-23', 1, 'EXPENSE', 125, 'Cinema with friends', 8);
const loans = [
  { id: id(6, 1), name: 'Loan to Sam', amount: 3000, type: 'LEND', accountId: accounts[1].id, dateTime: stamp('2026-09-12'), note: 'Fictional friend loan; 1,000 AED already repaid.' },
  { id: id(6, 2), name: 'Education loan', amount: 15000, type: 'BORROW', accountId: accounts[1].id, dateTime: stamp('2026-09-01'), note: 'Fictional principal with two 500 AED repayments.' },
];
transaction('2026-09-12', 1, 'EXPENSE', 3000, 'Lent to Sam', 9, { loanId: loans[0].id });
transaction('2026-09-01', 1, 'INCOME', 15000, 'Education loan received', 9, { loanId: loans[1].id });
const loanRecords = [
  { id: id(9, 1), loanId: loans[0].id, amount: 1000, dateTime: stamp('2026-10-19'), accountId: accounts[1].id, loanRecordType: 'DECREASE', interest: false },
  ...['09', '10'].map((month, i) => ({ id: id(9, i + 2), loanId: loans[1].id, amount: 500, dateTime: stamp(`2026-${month}-08`), accountId: accounts[1].id, loanRecordType: 'DECREASE', interest: false })),
];
loanRecords.forEach((record, i) => transaction(new Date(record.dateTime).toISOString().slice(0,10), 1, i === 0 ? 'INCOME' : 'EXPENSE', record.amount, i === 0 ? 'Sam repaid part of loan' : 'Education loan repayment', 9, { loanId: record.loanId, loanRecordId: record.id }));
transactions.sort((a,b) => a.dateTime - b.dateTime || a.id.localeCompare(b.id));
const budgets = [
  { id: id(3, 1), name: 'Groceries', amount: 1600, categoryIdsSerialized: categories[2].id, accountIdsSerialized: null, orderId: 0 },
  { id: id(3, 2), name: 'Eating out', amount: 700, categoryIdsSerialized: categories[3].id, accountIdsSerialized: null, orderId: 1 },
  { id: id(3, 3), name: 'Transport', amount: 600, categoryIdsSerialized: categories[4].id, accountIdsSerialized: null, orderId: 2 },
];
const goals = [
  { id: id(4, 1), name: 'Emergency cushion', kind: 'EMERGENCY_FUND', targetAmount: 20000, deadline: stamp('2027-06-30'), createdAt: created, updatedAt: created },
  { id: id(4, 2), name: 'New laptop', kind: 'PURCHASE', targetAmount: 6000, deadline: stamp('2027-03-01'), createdAt: created, updatedAt: created },
  { id: id(4, 3), name: 'Japan holiday', kind: 'SINKING_FUND', targetAmount: 12000, deadline: stamp('2027-10-01'), createdAt: created, updatedAt: created },
];
const goalAllocations = [[0,3,10000],[1,2,2500],[2,2,1500],[2,4,3000]].map(([g,a,amount]) => ({ goalId: goals[g].id, accountId: accounts[a].id, reservedAmount: amount, createdAt: created, updatedAt: created }));
const plannedPaymentRules = [
  { id: id(5,1), startDate: stamp('2026-11-01'), intervalN: 1, intervalType: 'MONTH', oneTime: false, type: 'INCOME', accountId: accounts[1].id, amount: 15000, categoryId: categories[0].id, title: 'Monthly salary' },
  { id: id(5,2), startDate: stamp('2026-11-02'), intervalN: 1, intervalType: 'MONTH', oneTime: false, type: 'EXPENSE', accountId: accounts[1].id, amount: 4500, categoryId: categories[5].id, title: 'Apartment rent' },
  { id: id(5,3), startDate: stamp('2026-10-27'), intervalN: 1, intervalType: 'MONTH', oneTime: false, type: 'EXPENSE', accountId: accounts[1].id, amount: 349, categoryId: categories[5].id, title: 'Home internet' },
  { id: id(5,4), startDate: stamp('2026-11-05'), intervalN: null, intervalType: null, oneTime: true, type: 'EXPENSE', accountId: accounts[1].id, amount: 350, categoryId: categories[7].id, title: 'Annual health check' },
];
const wealth = { version: 1, items: [], policies: [], valuations: [], rates: [], snapshots: [], reviews: [] };
function wealthItem(name, kind, source, sourceId, values, convention = 'ASSET') {
  const itemId = `demo-wealth-${wealth.items.length + 1}`;
  wealth.items.push({ id: itemId, name, kind, currency: 'AED', source, sourceId, convention, createdDate: '2026-09-01', note: 'Entirely fictional marketing example.', reviewDays: 90 });
  wealth.policies.push({ id: `${itemId}-policy`, itemId, effectiveDate: '2026-09-01', included: true, ownership: '100', archived: false, recordedAt: '2026-09-01T12:00:00Z' });
  if (values) ['2026-09-01','2026-10-01','2026-10-25'].forEach((day,i) => wealth.valuations.push({ id: `${itemId}-${day}`, itemId, effectiveDate: day, amount: String(values[i]), recordedAt: `${day}T12:00:00Z` }));
}
accounts.forEach((a,i) => wealthItem(a.name, i === 5 ? 'INVESTMENT' : i >= 2 ? 'SAVINGS' : 'CASH', 'ACCOUNT', a.id));
loans.forEach((loan,i) => wealthItem(loan.name, i === 0 ? 'RECEIVABLE' : 'STUDENT_LOAN', 'LOAN', loan.id, null, i === 0 ? 'ASSET' : 'POSITIVE_OWED'));
wealthItem('My car', 'VEHICLE', 'MANUAL', null, [46000,45500,45000]);
wealthItem('Gold savings', 'GOLD', 'MANUAL', null, [7800,8200,8500]);
wealthItem('Retirement fund', 'RETIREMENT', 'MANUAL', null, [72000,75000,78000]);
wealthItem('Credit card balance', 'CREDIT_CARD', 'MANUAL', null, [2400,2100,1800], 'POSITIVE_OWED');
for (const day of ['2026-09-01','2026-10-01','2026-10-25']) wealth.reviews.push({ id: `demo-review-${day}`, effectiveDate: day, assetsReviewed: true, debtsReviewed: true, recordedAt: `${day}T12:00:00Z` });
const backup = { accounts, categories, transactions, budgets, goals, goalAllocations, plannedPaymentRules, loans, loanRecords, wealth,
  settings: [{ id: id(7,1), theme: 'DARK', currency: 'AED', bufferAmount: 1500, name: 'Alex Morgan · Demo' }],
  sharedPrefs: { show_notifications: 'false', hide_current_balance: 'false', hide_income: 'false', transfers_as_inc_exp: 'false' } };
const filename = 'Jvy_Fictional_Marketing_2026-10.json';
const payload = JSON.stringify(backup, null, 2) + '\n';
fs.writeFileSync(path.join(out, filename), payload);
const balances = Object.fromEntries(accounts.map(a => [a.name, 0]));
for (const tx of transactions) {
  const from = accounts.find(a=>a.id === tx.accountId);
  balances[from.name] += tx.type === 'INCOME' ? tx.amount : -tx.amount;
  if (tx.type === 'TRANSFER') balances[accounts.find(a=>a.id === tx.toAccountId).name] += tx.toAmount;
}
for (const k of Object.keys(balances)) balances[k] = Number(balances[k].toFixed(2));
const summary = { fictional: true, asOf: '2026-10-25', baseCurrency: 'AED', encoding: 'UTF-8',
  accounts: accounts.length, transactions: transactions.length, budgets: budgets.length, goals: goals.length,
  reservations: goalAllocations.length, plannedPayments: plannedPaymentRules.length, loans: loans.length,
  netWorthItems: wealth.items.length, accountBalances: balances,
  totalAccountBalance: Number(Object.values(balances).reduce((a,b)=>a+b,0).toFixed(2)),
  expectedNetWorth: Number((Object.values(balances).reduce((a,b)=>a+b,0) + 2000 - 14000 + 45000 + 8500 + 78000 - 1800).toFixed(2)),
  sha256: crypto.createHash('sha256').update(payload).digest('hex'),
  warning: 'Use a separate test installation/device. Normal backup restore merges records and imports demo settings; never mix with real finances. Export your real backup first. No credentials are included or changed.' };
fs.writeFileSync(path.join(out, 'manifest.json'), JSON.stringify(summary, null, 2) + '\n');
const packaging = fs.mkdtempSync(path.join(out, 'package-'));
try {
  const archive = path.join(packaging, 'Jvy_Fictional_Marketing_2026-10.zip');
  const zipped = spawnSync('zip', ['-X', '-j', archive, path.join(out, filename)], { encoding: 'utf8' });
  if (zipped.status !== 0) throw new Error('JSON generated; ZIP packaging failed. Install zip or use the JSON directly.');
  fs.renameSync(archive, path.join(out, 'Jvy_Fictional_Marketing_2026-10.zip'));
} finally {
  // Only this generator invocation's uniquely created packaging directory.
  fs.rmSync(packaging, { recursive: true, force: true });
}
console.log(JSON.stringify({ directory: out, ...summary }, null, 2));
