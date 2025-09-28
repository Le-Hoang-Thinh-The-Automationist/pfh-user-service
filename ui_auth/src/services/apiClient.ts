// src/lib/apiClient.ts

const API_BASE_URL = import.meta.env.PUBLIC_API_URL || "http://localhost:5000";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface ApiResponse<T> {
  data?: T;
  error?: string;
  status: number;
}

export const apiClient = {
  // HTTP METHOD: GET 
  get: <T>(endpoint: string, headers?: HeadersInit) =>
    request<T>(endpoint, "GET", undefined, headers),
  // HTTP METHOD: POST 
  post: <T>(endpoint: string, body?: unknown, headers?: HeadersInit) =>
    request<T>(endpoint, "POST", body, headers),
  // HTTP METHOD: PUT 
  put: <T>(endpoint: string, body?: unknown, headers?: HeadersInit) =>
    request<T>(endpoint, "PUT", body, headers),
  // HTTP METHOD: DELETE 
  delete: <T>(endpoint: string, headers?: HeadersInit) =>
    request<T>(endpoint, "DELETE", undefined, headers),
};

async function request<T>(
  endpoint: string,
  method: HttpMethod,
  body?: unknown,
  headers: HeadersInit = {}
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      credentials: "include", 
    });

    const contentType = response.headers.get("Content-Type");
    const payload = contentType?.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      return {
        status: response.status,
        error: (payload as any)?.message || response.statusText,
      };
    }

    return { status: response.status, data: payload };
  } catch (err) {
    return {
      status: 500,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}


