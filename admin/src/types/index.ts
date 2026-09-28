export const UserStatus = {
  ACTIVE: 'ACTIVE',
  BLOCKED: 'BLOCKED'
} as const;
export type UserStatus = typeof UserStatus[keyof typeof UserStatus];

export const PaymentStatus = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  REFUNDED: 'REFUNDED'
} as const;
export type PaymentStatus = typeof PaymentStatus[keyof typeof PaymentStatus];

export const SubscriptionStatus = {
  ACTIVE: 'ACTIVE',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED'
} as const;
export type SubscriptionStatus = typeof SubscriptionStatus[keyof typeof SubscriptionStatus];

export const ScanStatus = {
  PENDING: 'PENDING',
  CLEAN: 'CLEAN',
  THREAT_FOUND: 'THREAT_FOUND',
  ERROR: 'ERROR'
} as const;
export type ScanStatus = typeof ScanStatus[keyof typeof ScanStatus];

export interface User {
  id: number;
  telegramUserId: number;
  username?: string;
  firstName?: string;
  email?: string;
  status: UserStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface Plan {
  id: number;
  name: string;
  price: number;
  scanLimit: number;
  durationDays: number;
  active: boolean;
}

export interface Subscription {
  id: number;
  user: User;
  plan: Plan;
  credits: number;
  version: number;
  startDate: string;
  endDate?: string;
  status: SubscriptionStatus;
}

export interface Payment {
  id: number;
  user: User;
  plan: Plan;
  amount: number;
  currency: string;
  provider: string;
  transactionId?: string;
  status: PaymentStatus;
  createdAt: string;
}

export interface Scan {
  id: number;
  user: User;
  fileName: string;
  fileSize: number;
  mimeType?: string;
  sha256: string;
  scanStatus: ScanStatus;
  threatName?: string;
  createdAt: string;
}