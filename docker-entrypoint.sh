#!/bin/sh
set -eu
state=/var/lib/proto-nova/tls-request
inbox=/etc/proto-nova/tls-request
mkdir -p "$state"
chmod 700 "$state"
# Import a new download once; never overwrite an automatically renewed chain
# with the unchanged original file when a container restarts.
if [ -f "$inbox/server-https.pem" ]; then
    digest=$(sha256sum "$inbox/server-https.pem" | cut -d ' ' -f 1)
    previous=$(cat "$state/.imported-bundle.sha256" 2>/dev/null || true)
    if [ "$digest" != "$previous" ]; then
        umask 077
        cp "$inbox/server-https.pem" "$state/.server-https.pem.new"
        mv "$state/.server-https.pem.new" "$state/server-https.pem"
        printf '%s' "$digest" > "$state/.imported-bundle.sha256"
    fi
fi
if [ -f "$inbox/auto-update.json" ]; then
    umask 077
    cp "$inbox/auto-update.json" "$state/.auto-update.json.new"
    mv "$state/.auto-update.json.new" "$state/auto-update.json"
fi
exec "$@"
