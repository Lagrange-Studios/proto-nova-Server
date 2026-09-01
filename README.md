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
