# Proto Nova Server

## Development

Run the repository-level `run.bat` to build the local game, launcher, and
development server together. To run only the dedicated server:

```text
gradle run --args="--headless"
```

Useful startup options:

- `--headless` or `--nogui`: terminal-only server suitable for hosting.
- `--gui`: desktop console.
- `--init-config`: create `proto-nova.properties` without starting a world.
- `--check-config`: strictly validate the configuration and exit.
- `--healthcheck`: check the local configured game listener.
- `--help` and `--version`: command information.

In the headless console, use `help`, `status`, `save`, and `stop`. The `stop`
command and operating-system shutdown hook save the world before closing.

Set only `game.socket.port`. The HTTPS status and signed client-download
listener automatically uses the port immediately below it. For example, base
port `8125` requires TCP `8125` and `8124` to be forwarded, while players enter
only `host:8125` in the Launcher.

## Distribution

Use the Proto-Nova Packager to make a server release. A packaged server includes
a minimized Java runtime, its signed downloadable game client, a detailed server
guide, headless setup scripts, backup tools, and side-by-side update/import
helpers. Docker packages include equivalent management commands and persistent
storage.

Do not distribute a development checkout: it may contain test worlds, server
identity files, logs, or credentials. The packager explicitly excludes them.

## Publicly trusted HTTPS (Let's Encrypt)

For a public server, point `play.proto-nova.net` (or your chosen hostname) at
its public IP. Players and browsers must connect using that same hostname.
A certificate for `proto-nova.net` alone does not cover `play.proto-nova.net`.
Every independently hosted server needs a hostname it controls; do not share
private keys with server operators.

On Linux, install Certbot using https://certbot.eff.org/instructions for your
OS. With DNS pointing at this machine and inbound TCP 80 available, obtain a
certificate (replace the example email with your real address):

```sh
sudo certbot certonly --standalone --domain play.proto-nova.net --email YOUR_EMAIL --agree-tos
```

If TCP 80 cannot reach the host, use a Certbot DNS provider plugin with automated
DNS validation instead. The game's ports do not satisfy the HTTP-01 challenge.
Configure the server's `proto-nova.properties`:

```properties
tls.certificate.path=/etc/letsencrypt/live/play.proto-nova.net/fullchain.pem
tls.private.key.path=/etc/letsencrypt/live/play.proto-nova.net/privkey.pem
```

The full chain must be in leaf-first order. RSA and EC unencrypted PEM private
keys are supported. Give only the server's service account read access to the
key and parent directories. Do not make it world-readable or run the game as
root. Alternatively, use a Certbot deploy hook to securely copy the chain and
key into a directory owned by that account (directory mode 0700, files 0600)
and configure those paths.

Restart the server after installation. Both the game and companion HTTPS
listeners use this certificate. Open `https://play.proto-nova.net:7674/status`
for the default base game port 7675. Keep both game and companion ports forwarded.
Invalid, expired, missing, or mismatched configured PEM files fail startup;
the server does not silently generate a replacement self-signed certificate.
Leaving both PEM settings empty preserves local self-signed operation.

Enable Certbot's renewal timer using the instructions for your installation.
Install a deploy hook that copies renewed files if needed and restarts your
actual server service, for example `systemctl restart proto-nova.service` if
that is its service name. Existing listeners load certificates at startup,
so renewal requires a restart. Verify with `sudo certbot renew --dry-run`.
Use a maintenance window because restarting disconnects players.

For Docker, mount the entire `/etc/letsencrypt` directory read-only (the `live`
files are symlinks into `archive`), or mount a restricted directory containing
copies of both PEM files. Configure paths visible inside the container and
ensure its non-root user can read them. Restart the container from the renewal
deploy hook. Do not bake private keys into images or release packages.
On Windows, an ACME client that exports PEM files can provide the same two
settings; configure its renewal action to restart the server.

Updated Launcher and Client builds validate public CA chains and the hostname
without certificate pins, so normal CA renewal needs no identity-reset prompt.
Self-signed servers retain the existing pin flow. Deploy updated clients along
with this server change; older clients still pin each certificate.

## Request a certificate through Proto Nova

Server owners can request a certificate at https://proto-nova.net/certificates.html.
An admin must approve every issuance and renewal. Use the managed hostname in
your approved server listing, for example `myserver.servers.proto-nova.net:7675`.
Generate the signing request with the updated server:

```text
Proto-Nova-Server --certificate-request=myserver.servers.proto-nova.net
```

Upload `tls-request/server-request.csr` on the website and explain your request.
Keep `tls-request/privkey.pem` private on your server. Once an admin approves the
request and issuance succeeds, download `fullchain.pem` into `tls-request` and set:

```properties
tls.certificate.path=tls-request/fullchain.pem
tls.private.key.path=tls-request/privkey.pem
```

Restart the server and connect using the approved hostname. Before expiration,
repeat the command (it preserves your private key) and submit a new request for
admin review. The website never automatically approves renewals. Test certificates
issued in staging are labelled and will still produce browser trust warnings.

### Automatic certificate updates

After the initial production certificate has issued, enable automatic updates on
its website request and download the private enrollment JSON. Run
`--certificate-auto-update=path/to/auto-update.json`, then start the server with its
PEM certificate/private-key paths configured. The server checks hourly, validates
renewals and reloads TLS for new connections. Keep the enrollment JSON and private
key in `tls-request` across updates. Renewals are provisionally accepted for admin
review; denial disables automation and revokes an issued automatic renewal.
