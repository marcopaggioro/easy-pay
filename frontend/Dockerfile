FROM node:23.11.0-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build

FROM joseluisq/static-web-server:2-alpine

COPY --from=build /app/dist/frontend/browser /public

ENV SERVER_CONFIG_FILE=/config/server.toml

RUN mkdir -p /config

# Page fallback prevents 404 when loading pages
RUN printf '[general]\n\
    page-fallback = "/public/index.html"\n\
    [advanced]\n\
    [[advanced.headers]]\n\
    source = "**"\n\
    headers.X-Service = "frontend"' >> /config/server.toml
