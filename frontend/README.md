# DANASEA frontend

## Google Login configuration

Use a Google OAuth **Web application** client. In the repository-root `.env`,
set `VITE_GOOGLE_CLIENT_ID` to the same Web client ID as the backend's
`GOOGLE_CLIENT_ID`. Vite reads this directory through `envDir: '../'` for local
development and embeds the value into the production JavaScript during build.
Restart the Vite dev server after changing it.

For a local Docker build, run `docker compose build frontend` from the repository
root. Compose passes `VITE_GOOGLE_CLIENT_ID` as a build argument. If left unset,
the build succeeds with a warning and Google Sign-In is gracefully disabled in the UI.
In CD or production builds, supply `REQUIRE_GOOGLE_CLIENT_ID=true` to fail fast if the ID is missing.
For a direct build, supply `--build-arg VITE_GOOGLE_CLIENT_ID=<your-web-client-id>` to `docker build`.
The repository-root `.env` stays outside the frontend build context.

For GitHub CD, add a **repository Actions variable** named `VITE_GOOGLE_CLIENT_ID`
under **Settings → Secrets and variables → Actions → Variables**. The CD build job
passes this variable to the frontend Docker build. Use a repository variable,
because the image build job does not use the `production` environment. The client
ID is public; `GOOGLE_CLIENT_SECRET` and other backend secrets must stay out of
frontend build arguments and all `VITE_*` variables.

The production Compose file uses prebuilt images. Changing the server's `.env`
or restarting its frontend container does not change the embedded client ID;
build and deploy a new frontend image after updating it.

In Google Cloud Console, add `https://danasea.tailfc3563.ts.net` to the client's
**Authorized JavaScript origins**. For local development, add `http://localhost`
and `http://localhost:3000` (or the actual local port). The Nginx CSP allows the
Google Identity Services script, styles, frames and connections. Reference:
[Google setup guide](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid).

After deploying the new image, verify that the document response has the updated
CSP, `/auth` loads `https://accounts.google.com/gsi/client` without a CSP violation,
the Google button no longer reports a missing client ID, and a valid Google
credential can complete `POST /api/auth/oauth2/google`.

## React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
