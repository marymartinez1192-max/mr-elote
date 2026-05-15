# syntax=docker/dockerfile:1.7
FROM nginx:1.27-alpine

# Static site
COPY index.html admin.html cart.html catalog.html orders.html /usr/share/nginx/html/
COPY css /usr/share/nginx/html/css
COPY js  /usr/share/nginx/html/js

# Nginx config (reverse proxy /api/v1 -> backend)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Runtime BASE_URL rewrite
COPY docker-entrypoint.sh /docker-entrypoint.d/40-rewrite-api-base.sh
RUN chmod +x /docker-entrypoint.d/40-rewrite-api-base.sh

ENV API_BASE_URL=/api/v1
EXPOSE 80
