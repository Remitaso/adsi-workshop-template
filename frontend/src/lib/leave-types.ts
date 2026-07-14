export type LeaveType = "PAID" | "HALF_DAY" | "COMPENSATORY" | "SPECIAL" | "ABSENCE";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface LeaveResponse {
  id: number;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: ApprovalStatus;
  rejectReason: string | null;
  createdAt: string;
}

export interface LeaveBalanceDetail {
  grantDate: string;
  expiryDate: string;
  granted: number;
  used: number;
  remaining: number;
}

export interface LeaveBalanceResponse {
  totalRemaining: number;
  details: LeaveBalanceDetail[];
}

export interface CreateLeaveRequest {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
}

export const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  PAID: "有給休暇",
  HALF_DAY: "半休",
  COMPENSATORY: "代休",
  SPECIAL: "特別休暇",
  ABSENCE: "欠勤",
};

export const STATUS_LABELS: Record<ApprovalStatus, string> = {
  PENDING: "承認待ち",
  APPROVED: "承認済み",
  REJECTED: "却下",
};
