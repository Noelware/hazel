# 🪶 hazel plugins
This is the plugins feature of hazel to tap in and do anything related to the **hazel** lifecycle.

## How does it work?
The `ClassLoader` for this is isolated from the main class-loader, it'll load from:

- `HAZEL_PLUGINS_PATH` environment variable,
- `hazel.plugins_dir` config variable,
- `./plugins` directory

## Projects
- [`:plugins:core`](./core) - The core mechanism to load and unload plugins
- [`:plugins:gradle-plugin`](./gradle-plugin) - 
- [`:plugins:maven-shields`](./maven-shields) - A plugin to provide routes for Maven repositories to have a "downloads count" badge.
- [`:plugins:redis`](./redis) - A plugin to store external plugin data to Redis
- [`:plugins:registry`](./registry) - Standalone project for `hazel.floofy.dev/registry`.
- [`:plugins:test`](./test) - Testing framework

## How do I build my own?
You can use the `dev.floofy.hazel:hazel-plugins-core` dependency in a Gradle or Maven project using the Noel Maven repository: `maven.floofy.dev/repo/[releases|snapshots]`

To add to the registry (and getting it approved), you will need to create an account at https://hazel.floofy.dev/registry, which
you can create an account.

All plugins submitted are hand-picked by Noel, so please do not spam submit or spam my email/dms! ;w;

You are expected to have a `plugin.json` file in the `resources/` directory with the following:

```javascript
{
    "name": "plugin-name",
    "description": "plugin description~",
    "version": "plugin version (must be valid SemVer)",
    "class": "my.example.PluginClass",
    "dependencies": {
        "plugin-name": "..."
    }
}
```

The **Gradle Plugin** will install the dependencies from `hazel.floofy.dev/registry/<name>/plugin.jar`
