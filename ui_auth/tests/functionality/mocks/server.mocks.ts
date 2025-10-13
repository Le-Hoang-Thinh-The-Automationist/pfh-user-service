import { setupServer } from "msw/node";
import { handlers } from "./handlers/handlers.mock.ts";

export const server = setupServer(...handlers);

// Vitest lifecycle hooks
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
