Welcome to the proto nova server program. Use the exe to start the server and type help to get all the commands.
It should automaticly publish the server to your local network but if you want players outside of your network you will have to port forward or use another method.
To get your server put on the public server list use the website proto-nova.net to request your server be put up.
Tip: deleting the world root will reset the server just make sure to close it before you delete and then re open the server

## Survival-only servers

Set the following property in `proto-nova.properties`, then restart the server:

```properties
game.cataclysm.enabled=false
```

The world will continue running normally, but cataclysm events and their win
condition will not advance. Set it back to `true` and restart to re-enable them.
