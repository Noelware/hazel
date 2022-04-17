# 🪶 ヘイゼル hazel •
> *Minimal, simple, and open source content delivery network made in Kotlin*

## Why?
**hazel** (case-sensitive) was in mind of being a reliable CDN service to be simple to configure without doing anything but
hosting your content from different providers.

**hazel** supports the following providers:

- Using the local **filesystem**,
- Using **Amazon S3** or an Amazon S3 compatible service

## Installation
You can install **hazel** through [Docker](#docker), the [Helm Chart](#helm-chart), or locally with [Git](#git).

### Prerequisites
You don't need much to install **hazel** onto your servers, all you really need is:

- IntelliJ / Eclipse (if contributing)
- Java 17 (if compiling)

This isn't really needed with the Docker or Helm Chart installation since the image contains the JDK runtime to
run **hazel**.

### Helm Chart
**hazel** is supported as a Helm Chart installation, the source can be found in the [charts](./charts) directory.

You are required to have a Kubernetes cluster running >=v1.23 and using Helm 3 or higher.

To get started, you must index from `charts.noelware.org`:

```shell
$ helm repo add noel https://charts.noelware.org/noel
```

Then, you install **hazel** on your Kubernetes cluster:

```shell
$ helm install hazel noel/hazel
```

This bootstraps with the **Filesystem** as the default provider, if you wish to use S3 or Google Cloud Storage, check the
[documentation](./charts/docs.md) for more information.

### Docker
You can install **hazel** with the Docker images provided on [Docker Hub](https://hub.docker.com/r/auguwu/hazel) or
on the [GitHub Container Registry]().

```shell
# Pull the Docker image from somewhere.
$ docker pull auguwu/hazel:latest

# Run it!
$ docker run -d -p 4949:4949 -v ~/some/path:/app/noel/hazel/data -v ~/config.toml:/app/noel/hazel/config.toml auguwu/hazel
```

### Locally with Git
```shell
$ git clone https://github.com/auguwu/hazel && cd hazel
$ ./gradlew installDist
$ ./build/install/hazel/bin/hazel
```

## Configuration
**hazel** can be configured without any configuration file, but if you want to customize anything, you can with a **TOML** file
that will be searched in the root directory or under `HAZEL_CONFIG_PATH` environment variable.

```toml
# Enables Sentry on the server, you will get errors and transactions on http requests and such.
sentry_dsn = ...

# Configures the Netty engine for Ktor, read more here:
# https://ktor.io/docs/engines.html#embedded-server-configure
[server]
# If the server should add additional headers for security reasons:
#    - X-Frame-Options
#        - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options
#        - Value: "deny"
#
#    - X-Content-Type-Options
#        - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
#        - Value: "nosniff"
#
#    - X-XSS-Protection
#        - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection
#        - Value: "1; mode=block"
security_headers = true

# Size of the queue to store all the application call instances that
# cannot be immediately processed.
request_queue_limit = 16

# Number of concurrently running requests from the same HTTP pipeline
running_limit = 10

# Do not create separate call event groups and reuse worker groups for
# processing calls.
share_work_group = false

# Timeout in seconds for sending responses to the client.
response_write_timeout_seconds = 10

# Timeout in seconds to read incoming requests from the client, "0" = infinite.
request_read_timeout = 0

# If this is set to `true`, this will enable TCP keep alive for connections
# that are so-called "dead" and can be easily discarded. The timeout period is configured
# by the system, so configure the end host accordingly.
keep_alive = false

# Extra headers to supply when the response is being created.
extra_headers = { server = "Noel/Hazel (+{{.Version}}; {{.GitRepo}})" }

# The host to use when creating a listener for the server to act on. By default,
# it uses `0.0.0.0`, which will be open to everyone if the `port` was forwarded.
#
# This property can be overridden by the "HOST" environment variable, or the
# "HAZEL_HOST" environment variable.
host = "0.0.0.0"

# The port to use when creating a listener for the server to act on. By default,
# it will allocate 4949, but this can be overridden by the "PORT" environment variable,
# or the "HAZEL_PORT" environment variable.
port = 4949

# This holds the Java keystore that hazel uses for users that can be connected
# to the client frontend.
[keystore]
# The path to the keystore that is used.
file = "path to .jks store"

# The password to use to the keystore
password = "owoing da uwuers!"

# This configures the storage class hazel will use.
[storage]
# Returns the storage class to use when operating on the server. By default,
# it will use the Filesystem.
class = "filesystem"

# The configuration for the filesystem configuration.
[storage.filesystem]
# The data point to do operations when requested on the server.
directory = "./data"

# The configuration for the S3 storage configuration
[storage.s3]
# The access key ID for S3. You can also add this in the keystore for extra
# security. Run `hazel keystore add storage.s3.access_key_id [value]` or
# `echo "access_key_id" | hazel keystore add storage.s3.access_key_id --stdin`
# to add it. You can also mask it as an environment variable with `${}`.
#
# Example:
#    access_key_id = "${S3_ACCESS_KEY_ID}"
#
# hazel will check if the environment variable exists or not. If not, it will throw
# an error when the server is being ran.
access_key_id = "..."

# The secret key for S3. You can also add this in the keystore for extra
# security. Run `hazel keystore add storage.s3.secret_key [value]` or
# `echo "secret_key" | hazel keystore add storage.s3.secret_key --stdin`
# to add it. You can also mask it as an environment variable with `${}`.
#
# Example:
#    secret_key = "${S3_SECRET_KEY}"
#
# hazel will check if the environment variable exists or not. If not, it will throw
# an error when the server is being ran.
secret_key = "..."

# The endpoint for the S3 server to use. This field can be omitted if
# you're using Amazon S3.
#
# For Wasabi users, you can use `s3.wasabisys.com` for this field to connect to Wasabi.
endpoint = ""

# The bucket to use to collect the items inside of the S3 bucket.
bucket = ""

# The region of where the bucket resides in.
region = "us-east-1"

# The default object canned ACL for new S3 objects.
default_object_canned_acl = "public-read"

# The default bucket canned ACL for the `config.storage.s3.bucket` option IF the bucket
# doesn't exist.
default_bucket_canned_acl = "public-read-write"
```

## Contributing
Thanks for considering contributing to **hazel**! Before you boop your heart out on your keyboard ✧ ─=≡Σ((( つ•̀ω•́)つ, we recommend you to do the following:

- Read the [Code of Conduct](./.github/CODE_OF_CONDUCT.md)
- Read the [Contributing Guide](./.github/CONTRIBUTING.md)

If you read both if you're a new time contributor, now you can do the following:

- [Fork me! ＊*♡( ⁎ᵕᴗᵕ⁎ ）](https://github.com/auguwu/hazel/fork)
- Clone your fork on your machine: `git clone https://github.com/your-username/hazel`
- Create a new branch: `git checkout -b some-branch-name`
- BOOP THAT KEYBOARD!!!! ♡┉ˏ͛ (❛ 〰 ❛)ˊˎ┉♡
- Commit your changes onto your branch: `git commit -am "add features （｡>‿‿<｡ ）"`
- Push it to the fork you created: `git push -u origin some-branch-name`
- Submit a Pull Request and then cry! ｡･ﾟﾟ･(థ Д థ。)･ﾟﾟ･｡

## License
**hazel** is released under the **Apache 2.0** License and with love :purple_heart: by [Noel](https://floofy.dev). :3
