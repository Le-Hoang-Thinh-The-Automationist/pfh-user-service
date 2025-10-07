/// <reference types="vitest" />
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globals: true,          // allows beforeAll, test, etc.
    environment: "jsdom",   // needed for React/Astro components
    setupFiles: ["./tests/functionality/mocks/server.mocks.ts"], // if you’re using MSW
  },
});
