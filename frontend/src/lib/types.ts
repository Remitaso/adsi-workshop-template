export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  role: "EMPLOYEE" | "MANAGER" | "HR";
  teamName: string;
}

export interface TimeEntryResponse {
  id: number;
  timeRecordId: number;
  clockIn: string;
  clockOut: string | null;
}

export interface TodayAttendanceResponse {
  workDate: string;
  status: string | null;
  entries: EntryDto[];
  totalWorkMinutes: number;
}

export interface EntryDto {
  id: number;
  clockIn: string;
  clockOut: string | null;
}

export interface MonthlyRecordsResponse {
  yearMonth: string;
  records: DailyRecord[];
}

export interface DailyRecord {
  workDate: string;
  status: string;
  totalWorkMinutes: number;
  overtimeMinutes: number;
  entries: EntryDto[];
}
