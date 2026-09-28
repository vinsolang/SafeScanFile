import React, { useState, useEffect } from 'react';
import {
  Users,
  ShieldAlert,
  CreditCard,
  Package,
  Activity,
  Plus,
  Ban,
  CheckCircle,
  RefreshCw,
  Search,
  AlertTriangle,
  FileCheck
} from 'lucide-react';
import {
  UserStatus,
  type User,
  type Plan,
  type Subscription,
  type Payment,
  type Scan,
  ScanStatus,
  type PaymentStatus
} from './types';
import { adminApi } from './services/api';

export default function App() {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'plans' | 'subscriptions' | 'payments' | 'scans'>('dashboard');
  
  // Data States
  const [users, setUsers] = useState<User[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [scans, setScans] = useState<Scan[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [search, setSearch] = useState<string>('');

  // Plan Modal State
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [newPlan, setNewPlan] = useState<Omit<Plan, 'id'>>({
    name: '',
    price: 0,
    scanLimit: 50,
    durationDays: 30,
    active: true,
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [uRes, pRes, subRes, payRes, scRes] = await Promise.allSettled([
        adminApi.getUsers(),
        adminApi.getPlans(),
        adminApi.getSubscriptions(),
        adminApi.getPayments(),
        adminApi.getScans(),
      ]);

      if (uRes.status === 'fulfilled') setUsers(uRes.value.data);
      if (pRes.status === 'fulfilled') setPlans(pRes.value.data);
      if (subRes.status === 'fulfilled') setSubscriptions(subRes.value.data);
      if (payRes.status === 'fulfilled') setPayments(payRes.value.data);
      if (scRes.status === 'fulfilled') setScans(scRes.value.data);
    } catch (err) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await adminApi.createPlan(newPlan);
      setShowPlanModal(false);
      setNewPlan({ name: '', price: 0, scanLimit: 50, durationDays: 30, active: true });
      fetchData();
    } catch (err) {
      alert('Failed to create plan');
    }
  };

  const handleToggleUserStatus = async (user: User) => {
    const nextStatus = user.status === UserStatus.ACTIVE ? UserStatus.BLOCKED : UserStatus.ACTIVE;
    try {
      await adminApi.updateUserStatus(user.id, nextStatus);
      fetchData();
    } catch (err) {
      alert('Failed to update user status');
    }
  };

  // Helper formatting functions
  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="flex h-screen bg-slate-900 text-slate-100 font-sans">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-800 border-r border-slate-700 flex flex-col">
        <div className="p-6 border-b border-slate-700 flex items-center gap-3">
          <ShieldAlert className="w-8 h-8 text-indigo-400" />
          <span className="text-xl font-bold tracking-wide">SafeScan Admin</span>
        </div>

        <nav className="flex-1 p-4 space-y-1">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: Activity },
            { id: 'users', label: 'Users', icon: Users },
            { id: 'plans', label: 'Plans', icon: Package },
            { id: 'subscriptions', label: 'Subscriptions', icon: RefreshCw },
            { id: 'payments', label: 'Payments', icon: CreditCard },
            { id: 'scans', label: 'Scan Logs', icon: FileCheck },
          ].map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id as any)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors ${
                  active
                    ? 'bg-indigo-600 text-white'
                    : 'text-slate-400 hover:bg-slate-700 hover:text-slate-200'
                }`}
              >
                <Icon className="w-5 h-5" />
                {item.label}
              </button>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-700 text-xs text-slate-500">
          Backend API Status: <span className="text-emerald-400 font-semibold">Online</span>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-slate-800 border-b border-slate-700 px-8 flex items-center justify-between">
          <div className="relative w-72">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search records..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-9 pr-4 py-1.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200 placeholder-slate-500"
            />
          </div>
          <button
            onClick={fetchData}
            className="flex items-center gap-2 bg-slate-700 hover:bg-slate-600 px-3 py-1.5 rounded-lg text-sm transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </header>

        {/* View Content */}
        <div className="flex-1 overflow-auto p-8">
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              {/* Metric Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-slate-800 border border-slate-700 rounded-xl p-5">
                  <div className="text-slate-400 text-sm">Total Users</div>
                  <div className="text-3xl font-bold mt-2">{users.length}</div>
                </div>
                <div className="bg-slate-800 border border-slate-700 rounded-xl p-5">
                  <div className="text-slate-400 text-sm">Total Scans Performed</div>
                  <div className="text-3xl font-bold mt-2">{scans.length}</div>
                </div>
                <div className="bg-slate-800 border border-slate-700 rounded-xl p-5">
                  <div className="text-slate-400 text-sm">Threats Detected</div>
                  <div className="text-3xl font-bold text-rose-400 mt-2">
                    {scans.filter((s) => s.scanStatus === ScanStatus.THREAT_FOUND).length}
                  </div>
                </div>
                <div className="bg-slate-800 border border-slate-700 rounded-xl p-5">
                  <div className="text-slate-400 text-sm">Active Subscriptions</div>
                  <div className="text-3xl font-bold text-emerald-400 mt-2">
                    {subscriptions.filter((s) => s.status === 'ACTIVE').length}
                  </div>
                </div>
              </div>

              {/* Recent Scans Table */}
              <div className="bg-slate-800 border border-slate-700 rounded-xl p-6">
                <h3 className="text-lg font-semibold mb-4">Recent Scan Logs</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="border-b border-slate-700 text-slate-400 text-xs uppercase tracking-wider">
                        <th className="py-3 px-4">File Name</th>
                        <th className="py-3 px-4">Size</th>
                        <th className="py-3 px-4">Status</th>
                        <th className="py-3 px-4">Threat</th>
                        <th className="py-3 px-4">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-700/50 text-sm">
                      {scans.slice(0, 5).map((scan) => (
                        <tr key={scan.id} className="hover:bg-slate-750">
                          <td className="py-3 px-4 font-medium">{scan.fileName}</td>
                          <td className="py-3 px-4 text-slate-400">{formatBytes(scan.fileSize)}</td>
                          <td className="py-3 px-4">
                            <span
                              className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                                scan.scanStatus === ScanStatus.CLEAN
                                  ? 'bg-emerald-500/10 text-emerald-400'
                                  : scan.scanStatus === ScanStatus.THREAT_FOUND
                                  ? 'bg-rose-500/10 text-rose-400'
                                  : 'bg-amber-500/10 text-amber-400'
                              }`}
                            >
                              {scan.scanStatus}
                            </span>
                          </td>
                          <td className="py-3 px-4 text-rose-400">{scan.threatName || '—'}</td>
                          <td className="py-3 px-4 text-slate-400">
                            {new Date(scan.createdAt).toLocaleString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Users Tab */}
          {activeTab === 'users' && (
            <div className="bg-slate-800 border border-slate-700 rounded-xl p-6">
              <h3 className="text-lg font-semibold mb-4">Telegram Users</h3>
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-700 text-slate-400 text-xs uppercase">
                    <th className="py-3 px-4">ID</th>
                    <th className="py-3 px-4">Telegram ID</th>
                    <th className="py-3 px-4">Name / Username</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Joined</th>
                    <th className="py-3 px-4">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700 text-sm">
                  {users.map((u) => (
                    <tr key={u.id}>
                      <td className="py-3 px-4">{u.id}</td>
                      <td className="py-3 px-4 font-mono text-xs">{u.telegramUserId}</td>
                      <td className="py-3 px-4">
                        {u.firstName} {u.username && <span className="text-slate-400">(@{u.username})</span>}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-1 rounded text-xs font-medium ${
                            u.status === UserStatus.ACTIVE
                              ? 'bg-emerald-500/10 text-emerald-400'
                              : 'bg-rose-500/10 text-rose-400'
                          }`}
                        >
                          {u.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-400">{new Date(u.createdAt).toLocaleDateString()}</td>
                      <td className="py-3 px-4">
                        <button
                          onClick={() => handleToggleUserStatus(u)}
                          className={`p-1.5 rounded hover:bg-slate-700 transition-colors ${
                            u.status === UserStatus.ACTIVE ? 'text-rose-400' : 'text-emerald-400'
                          }`}
                          title={u.status === UserStatus.ACTIVE ? 'Block User' : 'Unblock User'}
                        >
                          {u.status === UserStatus.ACTIVE ? <Ban className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Plans Tab */}
          {activeTab === 'plans' && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="text-lg font-semibold">Subscription Plans</h3>
                <button
                  onClick={() => setShowPlanModal(true)}
                  className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                >
                  <Plus className="w-4 h-4" /> Add Plan
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {plans.map((p) => (
                  <div key={p.id} className="bg-slate-800 border border-slate-700 rounded-xl p-6 flex flex-col justify-between">
                    <div>
                      <div className="flex justify-between items-start">
                        <h4 className="text-xl font-bold">{p.name}</h4>
                        <span className={`px-2 py-0.5 rounded text-xs ${p.active ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-700 text-slate-400'}`}>
                          {p.active ? 'Active' : 'Disabled'}
                        </span>
                      </div>
                      <div className="text-3xl font-extrabold mt-4">${p.price.toFixed(2)}</div>
                      <ul className="mt-6 space-y-2 text-sm text-slate-300">
                        <li>• {p.scanLimit} scans per cycle</li>
                        <li>• Valid for {p.durationDays} days</li>
                      </ul>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Modal: Create Plan */}
      {showPlanModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-semibold">Create New Plan</h3>
            <form onSubmit={handleCreatePlan} className="space-y-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Plan Name</label>
                <input
                  type="text"
                  required
                  value={newPlan.name}
                  onChange={(e) => setNewPlan({ ...newPlan, name: e.target.value })}
                  className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={newPlan.price}
                    onChange={(e) => setNewPlan({ ...newPlan, price: parseFloat(e.target.value) })}
                    className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 text-sm text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Scan Limit</label>
                  <input
                    type="number"
                    required
                    value={newPlan.scanLimit}
                    onChange={(e) => setNewPlan({ ...newPlan, scanLimit: parseInt(e.target.value) })}
                    className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 text-sm text-slate-100"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1">Duration (Days)</label>
                <input
                  type="number"
                  required
                  value={newPlan.durationDays}
                  onChange={(e) => setNewPlan({ ...newPlan, durationDays: parseInt(e.target.value) })}
                  className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 text-sm text-slate-100"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowPlanModal(false)}
                  className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 rounded text-sm font-medium"
                >
                  Save Plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}