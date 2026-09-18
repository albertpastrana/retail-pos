# Grafana Alloy

This configuration monitors the Linux host running Retail POS, PostgreSQL, and
the POS log files. It sends metrics to Grafana Cloud Prometheus and logs to
Grafana Cloud Loki. It also exposes loopback-only OTLP receivers for optional
Java instrumentation.

The current POS launcher does not emit OTLP on its own. File logs are therefore
the authoritative application signal until a Java OpenTelemetry agent is
installed and enabled.

## Install on Debian or Ubuntu

On the prepared `casa-victoria` host, run the interactive installer as `cv`:

```sh
/home/cv/observability/alloy/install-alloy.sh
```

It prompts for Grafana Cloud values and the PostgreSQL monitoring password;
none of those secrets are embedded in the script.

Install the Grafana Alloy package using Grafana's documented apt repository,
then install these files:

```sh
sudo install -o root -g root -m 0644 observability/alloy/config.alloy /etc/alloy/config.alloy
sudo install -o root -g root -m 0600 observability/alloy/env.example /etc/alloy/env
sudoedit /etc/alloy/env
sudo systemctl restart alloy
sudo systemctl enable alloy
```

The package service must load the environment file. On installations where the
unit does not already do so, create a drop-in:

```ini
# /etc/systemd/system/alloy.service.d/retail-pos.conf
[Service]
EnvironmentFile=/etc/alloy/env
```

Then apply it:

```sh
sudo systemctl daemon-reload
sudo systemctl restart alloy
sudo systemctl --no-pager --full status alloy
```

Create the database monitoring role before starting Alloy:

```sql
CREATE USER grafana_monitor WITH PASSWORD 'replace-with-a-unique-password';
GRANT pg_monitor TO grafana_monitor;
```

Do not commit `/etc/alloy/env`; it contains the Grafana Cloud token and the
PostgreSQL monitoring password.

## Verify

Check Alloy logs locally:

```sh
sudo journalctl -u alloy -n 100 --no-pager
```

In Grafana Cloud Explore, query `node_uname_info`, `pg_up`, and Loki labels
`job="java-app"`, `job="postgres"`, and `job="journal"`.
