# syntax=docker/dockerfile:1
# Build context is the `frontend/` directory (see docker-compose.yml) so the build
# can reach ../shared, which the admin imports via the @shared alias.

# ---- build the admin SPA ----
FROM node:22-alpine AS build
WORKDIR /app
# Install deps first (cached unless package*.json change) — must come BEFORE copying
# shared/ or admin sources, otherwise unrelated changes bust the npm cache.
COPY admin/package.json admin/package-lock.json ./admin/
WORKDIR /app/admin
RUN --mount=type=cache,target=/root/.npm \
    npm ci --registry=https://registry.npmmirror.com
# Now bring in sources (admin imports ../shared via the @shared alias).
COPY shared /app/shared
COPY admin ./
RUN npm run build

# ---- serve with nginx ----
FROM nginx:1.27-alpine
COPY admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/admin/dist /usr/share/nginx/html
EXPOSE 80
