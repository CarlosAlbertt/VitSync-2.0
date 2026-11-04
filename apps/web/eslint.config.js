import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import tseslint from "typescript-eslint";

// Regla de la guía (Fase 9.2): una feature no importa de otra feature.
// Lo compartido sube a shared/.
const FEATURES = ["appointments", "auth", "billing", "catalog", "contact", "dashboard", "documents", "family", "health", "home", "messages", "profile", "reports", "settings"];

export default tseslint.config(
  { ignores: ["dist", "public/mockServiceWorker.js"] },
  ...tseslint.configs.recommended,
  {
    files: ["src/**/*.{ts,tsx}"],
    languageOptions: { globals: globals.browser },
    plugins: { "react-hooks": reactHooks },
    rules: {
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
    },
  },
  ...FEATURES.map((feature) => ({
    files: [`src/features/${feature}/**/*.{ts,tsx}`],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: FEATURES.filter((f) => f !== feature).map((f) => `@/features/${f}/*`),
              message: "Una feature no importa de otra: sube lo común a shared/.",
            },
          ],
        },
      ],
    },
  })),
);
