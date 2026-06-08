ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace/campus-life-frontend

COPY campus-life-frontend/package*.json ./
RUN npm ci

COPY campus-life-frontend/ ./
RUN npm run build

FROM ${NGINX_IMAGE}

COPY --from=build /workspace/campus-life-frontend/dist/consumer-web/apps/consumer-web/ /usr/share/nginx/consumer-web/
COPY --from=build /workspace/campus-life-frontend/dist/merchant-web/apps/merchant-web/ /usr/share/nginx/merchant-web/
COPY --from=build /workspace/campus-life-frontend/dist/admin-web/apps/admin-web/ /usr/share/nginx/admin-web/
COPY deploy/docker/frontend/default.conf.template /etc/nginx/templates/default.conf.template

RUN ln -sf apps/consumer-web/index.html /usr/share/nginx/consumer-web/index.html \
    && ln -sf apps/merchant-web/index.html /usr/share/nginx/merchant-web/index.html \
    && ln -sf apps/admin-web/index.html /usr/share/nginx/admin-web/index.html

EXPOSE 80
