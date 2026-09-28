import axios from 'axios';
import type { Plan, User, UserStatus } from '../types';

const API_BASE = 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

export const adminApi = {
  // Stats
  getDashboardStats: () => api.get('/admin/stats'),

  // Users
  getUsers: () => api.get('/admin/users'),
  updateUserStatus: (id: number, status: UserStatus) =>
    api.patch(`/admin/users/${id}/status`, { status }),

  // Plans
  getPlans: () => api.get('/admin/plans'),
  createPlan: (plan: Omit<Plan, 'id'>) => api.post('/admin/plans', plan),
  updatePlan: (id: number, plan: Partial<Plan>) => api.put(`/admin/plans/${id}`, plan),
  togglePlanActive: (id: number, active: boolean) =>
    api.patch(`/admin/plans/${id}/active`, { active }),

  // Subscriptions
  getSubscriptions: () => api.get('/admin/subscriptions'),
  addCredits: (subscriptionId: number, creditsToAdd: number) =>
    api.post(`/admin/subscriptions/${subscriptionId}/add-credits`, { credits: creditsToAdd }),

  // Payments
  getPayments: () => api.get('/admin/payments'),

  // Scans
  getScans: () => api.get('/admin/scans'),
};