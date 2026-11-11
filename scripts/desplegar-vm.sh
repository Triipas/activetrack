#!/usr/bin/env bash
# Despliega ActiveTrack en una VM Linux (Ubuntu/Debian) con Docker.
#
# Uso (desde la raíz del repositorio, en Linux, macOS o Git Bash en Windows):
#   scripts/desplegar-vm.sh usuario@IP_DE_LA_VM [ruta/a/clave-ssh]
#
# Requisitos: acceso SSH a la VM con un usuario con sudo y el archivo .env.prod (ver .env.prod.example).
# Se despliega el último commit (git archive HEAD): los cambios sin commitear no se suben.
set -euo pipefail

DESTINO="${1:?Uso: scripts/desplegar-vm.sh usuario@IP_DE_LA_VM [ruta/a/clave-ssh]}"
CLAVE_SSH="${2:-}"

cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo "Falta .env.prod: cópialo desde .env.prod.example y cambia las claves." >&2
  exit 1
fi
if grep -q "cambiar" .env.prod; then
  echo "En .env.prod quedan valores de ejemplo ('cambiar...'). Reemplázalos antes de desplegar." >&2
  exit 1
fi
for variable in DB_PASSWORD RABBIT_USER RABBIT_PASSWORD JWT_SECRET CLAVE_ORGANIZADOR CLAVE_JUEZ; do
  valor="$( (grep -E "^${variable}=" .env.prod || true) | tail -1 | cut -d= -f2- | tr -d '\r')"
  if [ -z "$valor" ]; then
    echo "Falta ${variable} en .env.prod." >&2
    exit 1
  fi
  if [ "$variable" = "JWT_SECRET" ] && [ "${#valor}" -lt 32 ]; then
    echo "JWT_SECRET debe tener al menos 32 caracteres." >&2
    exit 1
  fi
done

SSH_OPCIONES=(-o StrictHostKeyChecking=accept-new)
if [ -n "$CLAVE_SSH" ]; then
  SSH_OPCIONES+=(-i "$CLAVE_SSH")
fi

PAQUETE="$(mktemp -d)/activetrack.tar.gz"

echo "==> 1/3 Empaquetando el commit $(git rev-parse --short HEAD)"
git archive --format=tar.gz -o "$PAQUETE" HEAD

echo "==> 2/3 Copiando a $DESTINO"
scp "${SSH_OPCIONES[@]}" "$PAQUETE" .env.prod scripts/vm/remoto.sh "$DESTINO:~/"

echo "==> 3/3 Instalando y levantando en la VM (la primera vez tarda varios minutos)"
# El script se ejecuta desde un archivo (no por stdin) para que ningún comando consuma sus líneas.
ssh "${SSH_OPCIONES[@]}" "$DESTINO" 'bash ~/remoto.sh && rm -f ~/remoto.sh'

rm -f "$PAQUETE"
echo "==> Listo: http://${DESTINO#*@}"
