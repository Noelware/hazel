# 🪶 Hazel Plugin Registry
This is the source code for the backend of Hazel's plugin registry hosted at [hazel.noelware.org](https://hazel.noelware.org). This is just
a minimal API server made in Ktor that hosts and downloads content from Noelware's Artifacts Registry.

You will need a ClickHouse installation when running the registry since ClickHouse holds all the statistics (downloads, push, etc) database.

You can use the `hazel plugins publish` subcommand to publish your Hazel plugins. At the moment of 12/01/23, we don't allow people to add their plugins,
just yet. :3

## REST API Reference
```
# API URL
https://hazel.noelware.org/api

# Artifacts Registry
https://artifacts.noelware.cloud
```

### GET /
```json
{
  "success": true,
  "data": {
    "message": "Hello, world!"
  }
}
```

### GET /plugins

### GET /plugins/:pluginId

### GET /plugins/:pluginId/downloads

### GET /plugins/:pluginId/download
