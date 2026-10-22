FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=${VITE_GOOGLE_CLIENT_ID}

RUN if [ -z "$VITE_GOOGLE_CLIENT_ID" ]; then \
      echo "VITE_GOOGLE_CLIENT_ID is required to build the frontend image." >&2; \
      exit 1; \
    fi; \
    npm run build


FROM nginxinc/nginx-unprivileged:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build --chown=101:101 /app/dist /usr/share/nginx/html

EXPOSE 8080

USER 101

CMD ["nginx", "-g", "daemon off;"]
