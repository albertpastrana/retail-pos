#!/usr/bin/env bash
set -euo pipefail

CONFIG_DIR=/home/cv/observability/alloy
CONFIG_FILE=/etc/alloy/config.alloy
ENV_FILE=/etc/alloy/env

if [ "$(id -un)" != "cv" ]; then
  echo "Run this script as cv, not as root." >&2
  exit 1
fi

sudo -v

read -r -p "Grafana Cloud Prometheus remote_write URL: " prometheus_url
read -r -p "Grafana Cloud Prometheus username: " prometheus_user
read -r -p "Grafana Cloud Loki push URL: " loki_url
read -r -p "Grafana Cloud Loki username: " loki_user
read -r -p "Grafana Cloud OTLP username: " otlp_user
read -r -p "Grafana Cloud OTLP endpoint (host:443): " otlp_endpoint
read -r -s -p "Grafana Cloud API token: " grafana_token
printf '\n'
read -r -s -p "New grafana_monitor PostgreSQL password: " postgres_password
printf '\n'

if [ -z "$grafana_token" ] || [ -z "$postgres_password" ]; then
  echo "The Grafana token and PostgreSQL password are required." >&2
  exit 1
fi
if [[ ! "$postgres_password" =~ ^[A-Za-z0-9]+$ ]]; then
  echo "Use an alphanumeric PostgreSQL password only; it is placed in a URL." >&2
  exit 1
fi

echo "Installing Alloy package..."
sudo apt-get update
sudo apt-get install -y gpg wget acl
wget -q -O - https://apt.grafana.com/gpg.key \
  | gpg --dearmor \
  | sudo tee /etc/apt/trusted.gpg.d/grafana.gpg >/dev/null
echo "deb [signed-by=/etc/apt/trusted.gpg.d/grafana.gpg] https://apt.grafana.com stable main" \
  | sudo tee /etc/apt/sources.list.d/grafana.list >/dev/null
sudo apt-get update
sudo apt-get install -y alloy

echo "Creating PostgreSQL monitoring role..."
sudo -u postgres psql -v ON_ERROR_STOP=1 -v monitor_password="$postgres_password" -d pos \
  -c "ALTER ROLE grafana_monitor WITH LOGIN PASSWORD '$postgres_password';" 2>/dev/null \
  || sudo -u postgres psql -v ON_ERROR_STOP=1 -v monitor_password="$postgres_password" -d pos \
    -c "CREATE ROLE grafana_monitor LOGIN PASSWORD '$postgres_password';"
sudo -u postgres psql -v ON_ERROR_STOP=1 -d pos \
  -c "GRANT pg_monitor TO grafana_monitor;"

echo "Installing configuration..."
sudo install -d -o root -g root -m 0755 /etc/alloy
sudo install -o root -g root -m 0644 "$CONFIG_DIR/config.alloy" "$CONFIG_FILE"

tmp_env=$(mktemp)
trap 'rm -f "$tmp_env"' EXIT
umask 077
cat >"$tmp_env" <<EOF
GRAFANA_CLOUD_API_TOKEN=$grafana_token
GRAFANA_CLOUD_PROMETHEUS_URL=$prometheus_url
GRAFANA_CLOUD_PROMETHEUS_USERNAME=$prometheus_user
GRAFANA_CLOUD_LOKI_URL=$loki_url
GRAFANA_CLOUD_LOKI_USERNAME=$loki_user
GRAFANA_CLOUD_OTLP_USERNAME=$otlp_user
GRAFANA_CLOUD_OTLP_ENDPOINT=$otlp_endpoint
POSTGRES_MONITOR_DSN=postgresql://grafana_monitor:$postgres_password@127.0.0.1:5432/pos?sslmode=disable
EOF
sudo install -o root -g root -m 0600 "$tmp_env" "$ENV_FILE"

echo "Granting Alloy access to POS logs and the system journal..."
sudo setfacl -m u:alloy:--x /home/cv /home/cv/retail-pos
sudo setfacl -m u:alloy:rX /home/cv/retail-pos/logs
sudo setfacl -m d:u:alloy:rX /home/cv/retail-pos/logs
sudo usermod -aG adm alloy

sudo mkdir -p /etc/systemd/system/alloy.service.d
sudo tee /etc/systemd/system/alloy.service.d/retail-pos.conf >/dev/null <<'EOF'
[Service]
EnvironmentFile=/etc/alloy/env
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now alloy
sleep 3
sudo systemctl --no-pager --full status alloy
echo
echo "Recent Alloy logs:"
sudo journalctl -u alloy -n 50 --no-pager
