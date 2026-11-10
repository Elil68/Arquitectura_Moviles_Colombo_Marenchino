ARG NODE_IMAGE=node:lts-slim
FROM ${NODE_IMAGE}
ENV NODE_ENV=production HOST=0.0.0.0 PORT=3000 DATA_DIR=/app/datos
WORKDIR /app
COPY --chown=node:node server.js package.json ./
COPY --chown=node:node src ./src
COPY --chown=node:node public ./public
COPY --chown=node:node tools ./tools
RUN mkdir -p /app/datos && chown node:node /app/datos
USER node
VOLUME ["/app/datos"]
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s CMD node -e "fetch('http://127.0.0.1:3000/readyz').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node","server.js"]
