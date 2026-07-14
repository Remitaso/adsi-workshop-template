export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  role: "EMPLOYEE" | "MANAGER" | "HR";
  teamName: string;
}
