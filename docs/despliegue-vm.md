# Despliegue en una VM en la nube (HU-14)

ActiveTrack se despliega en **una máquina virtual Linux con Docker**: los mismos contenedores que se usan en desarrollo,
con ajustes de producción ([`docker-compose.prod.yml`](../docker-compose.prod.yml)):

- Solo el frontend (Nginx, puerto 80) queda expuesto a Internet. PostgreSQL, Redis, RabbitMQ y los servicios quedan en la red interna de Docker.
- Claves propias para la base de datos, RabbitMQ y la firma de los JWT; las claves de demostración de `organizador` y `juez` se reemplazan al desplegar.
- Datos de PostgreSQL en un volumen persistente y límites de memoria por contenedor.

## Despliegue actual en AWS

La instancia se crea con la plantilla [`infra/aws/activetrack-ec2.yaml`](../infra/aws/activetrack-ec2.yaml)
(CloudFormation): EC2 **t3.medium** con Ubuntu 24.04 en la VPC por defecto de `us-east-1`, disco gp3 cifrado de 30 GB,
IMDSv2 obligatorio, IP elástica, y un grupo de seguridad con el puerto 80 abierto y el 22 solo para la IP del administrador.
Docker y 2 GB de swap se instalan al arrancar (user-data).

```bash
aws ec2 create-key-pair --key-name activetrack-aws --key-type ed25519 --query KeyMaterial --output text > ~/.ssh/activetrack-aws.pem
aws cloudformation deploy --stack-name activetrack --template-file infra/aws/activetrack-ec2.yaml \
  --parameter-overrides KeyName=activetrack-aws SshCidr=$(curl -s https://checkip.amazonaws.com)/32
aws cloudformation describe-stacks --stack-name activetrack --query "Stacks[0].Outputs"
scripts/desplegar-vm.sh ubuntu@IP_ELASTICA ~/.ssh/activetrack-aws.pem
```

Costo aproximado: ~30 USD/mes por la instancia, más ~3,6 USD/mes por la IP pública y ~2,4 USD/mes por el disco.
Para eliminar todo y dejar de pagar:

```bash
aws cloudformation delete-stack --stack-name activetrack
aws ec2 delete-key-pair --key-name activetrack-aws
```

Si cambia tu IP pública, vuelve a ejecutar el `deploy` con el nuevo `SshCidr` para recuperar el acceso SSH.

## 1. Crear la VM (otros proveedores)

| Requisito | Valor |
|---|---|
| Sistema operativo | Ubuntu 22.04 o 24.04 (x86 o ARM) |
| Memoria | **4 GB** recomendados (el sistema usa ~1,1 GB en reposo; los límites suman 3,2 GB) |
| CPU / disco | 2 vCPU · 20 GB |
| Puertos abiertos | 22 (SSH) y 80 (HTTP) |

Cualquier proveedor sirve. Opciones habituales:

| Proveedor | Instancia sugerida | Costo aproximado |
|---|---|---|
| Oracle Cloud | VM.Standard.A1.Flex (ARM) 2 OCPU / 12 GB | Gratis (Always Free) |
| AWS EC2 | t3.medium (4 GB) | ~30 USD/mes (la capa gratuita, con 1 GB, no alcanza) |
| Azure | B2s (4 GB) | ~30 USD/mes (Azure for Students da crédito) |
| DigitalOcean | Droplet Basic 4 GB | ~24 USD/mes (GitHub Student Pack da crédito) |

Al crearla, agrega tu clave pública SSH y abre los puertos 22 y 80 en el firewall o grupo de seguridad.

## 2. Preparar las claves

En la raíz del repositorio:

```bash
cp .env.prod.example .env.prod
```

Edita `.env.prod` y reemplaza **todos** los valores (`JWT_SECRET` con al menos 32 caracteres aleatorios).
El archivo está en `.gitignore`: no se sube al repositorio.

## 3. Desplegar

```bash
scripts/desplegar-vm.sh ubuntu@IP_DE_LA_VM ~/.ssh/mi_clave
```

El script empaqueta el último commit, lo copia por SSH junto con `.env.prod`, instala Docker en la VM si falta
(script oficial `get.docker.com`), construye y levanta los contenedores, y cambia las claves de organizador y juez.
La primera vez tarda unos minutos porque compila los servicios dentro de la VM.

Al terminar, la aplicación queda en `http://IP_DE_LA_VM`.

### Desde GitHub Actions (opcional)

El workflow [`desplegar.yml`](../.github/workflows/desplegar.yml) ejecuta el mismo script desde GitHub.
Configura los secretos `VM_HOST`, `VM_USER`, `VM_SSH_KEY` y `ENV_PROD` (contenido de `.env.prod`) en
*Settings → Secrets and variables → Actions* y lánzalo desde *Actions → Desplegar en la VM → Run workflow*.

## 4. Verificar

```bash
curl -s http://IP_DE_LA_VM/api/catalogo/eventos
```

En la VM:

```bash
cd ~/activetrack && sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

## Operación

| Tarea | Comando (en la VM, dentro de `~/activetrack`) |
|---|---|
| Ver logs de un servicio | `sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f inscripciones-service` |
| Actualizar a un commit nuevo | volver a ejecutar `scripts/desplegar-vm.sh` desde tu máquina |
| Detener sin borrar datos | `sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml down` |
| Borrar también la base de datos | `sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml down -v` |

## Consideraciones

- **HTTPS**: para un dominio propio, agrega un proxy con certificado (por ejemplo Caddy o Certbot delante de Nginx).
  Sin dominio, la demo funciona por HTTP.
- **Recepción de lecturas RFID**: `POST /api/lecturas` queda abierto para el hardware y el simulador. En un evento real
  se restringiría por IP de las antenas o con una clave por antena.
- **Costo**: apaga o elimina la VM al terminar la evaluación para no generar cobros.
