export function withBasePath(path: string): string {
  const base = process.env.NEXT_PUBLIC_BASE_PATH || "";
  return `${base}${path}`;
}

interface ApiError {
  error: {
    code: string;
    message: string;
    details?: Array<{ field: string; message: string }>;
  };
}

class ApiClient {
  async fetch<T>(path: string, options?: RequestInit): Promise<T> {
    const url = withBasePath(`/api/v1${path}`);
    const response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options?.headers,
      },
      credentials: "include",
    });

    if (!response.ok) {
      const errorData: ApiError = await response.json().catch(() => ({
        error: { code: "UNKNOWN", message: "通信エラーが発生しました" },
      }));
      throw new ApiClientError(response.status, errorData.error);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  async get<T>(path: string): Promise<T> {
    return this.fetch<T>(path);
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.fetch<T>(path, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async put<T>(path: string, body: unknown): Promise<T> {
    return this.fetch<T>(path, {
      method: "PUT",
      body: JSON.stringify(body),
    });
  }

  async delete(path: string): Promise<void> {
    return this.fetch<void>(path, { method: "DELETE" });
  }
}

export class ApiClientError extends Error {
  constructor(
    public status: number,
    public error: { code: string; message: string; details?: Array<{ field: string; message: string }> }
  ) {
    super(error.message);
    this.name = "ApiClientError";
  }
}

export const apiClient = new ApiClient();
