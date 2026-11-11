#!/usr/bin/env bash
# Se ejecuta DENTRO de la VM (lo invoca scripts/desplegar-vm.sh por SSH).
set -euo pipefail

DIRECTORIO="$HOME/activetrack"
COMPOSE=(sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml)

if ! command -v docker >/dev/null 2>&1; then
  echo "--> Instalando Docker (script oficial get.docker.com)"
  curl -fsSL https://get.docker.com | sudo sh
fi

echo "--> Extrayendo el código en $DIRECTORIO"
mkdir -p "$DIRECTORIO"
tar -xzf "$HOME/activetrack.tar.gz" -C "$DIRECTORIO"
mv "$HOME/.env.prod" "$DIRECTORIO/.env"
chmod 600 "$DIRECTORIO/.env"
rm -f "$HOME/activetrack.tar.gz"
cd "$DIRECTORIO"

echo "--> Construyendo y levantando los contenedores"
"${COMPOSE[@]}" up -d --build --remove-orphans </dev/null
# Nginx lee su configuración (montada desde el código) solo al arrancar
"${COMPOSE[@]}" restart frontend </dev/null

echo "--> Esperando a la base de datos"
for _ in $(seq 1 60); do
  if "${COMPOSE[@]}" exec -T postgres pg_isready -U activetrack -d activetrack </dev/null >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "--> Reemplazando las claves de demostración de organizador y juez"
set -a
# shellcheck disable=SC1091
. ./.env
set +a
"${COMPOSE[@]}" exec -T postgres psql -U activetrack -d activetrack -v ON_ERROR_STOP=1 \
  -v clave_org="$CLAVE_ORGANIZADOR" -v clave_juez="$CLAVE_JUEZ" <<'SQL'
UPDATE usuario SET clave_hash = crypt(:'clave_org', gen_salt('bf', 10)) WHERE usuario = 'organizador';
UPDATE usuario SET clave_hash = crypt(:'clave_juez', gen_salt('bf', 10)) WHERE usuario = 'juez';
SQL

echo "--> Claves de organizador y juez actualizadas"
"${COMPOSE[@]}" ps --format 'table {{.Service}}\t{{.Status}}' </dev/null
