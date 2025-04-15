FROM node:23.11.0-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build

FROM joseluisq/static-web-server:2-alpine

COPY --from=build /app/dist/frontend/browser /public
