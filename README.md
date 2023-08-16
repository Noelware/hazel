# 🪶 hazel
> *Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints*

**hazel** is Noelware's HTTP proxy microservice to proxy all our storage provider items from our [Amazon S3](https://s3.amazonaws.com) to the web easily, reliability, and fast.

The following domains point to hazel directly:

- [artifacts.noelware.cloud](https://artifacts.noelware.cloud)
- [cdn.noelware.cloud](https://cdn.noelware.cloud)
- [cdn.floofy.dev](https://cdn.floofy.dev)

**hazel** is a graduated Noelware project, it was originally owned and created by [Noel](https://floofy.dev), but still maintained by him!

## Installation
### Docker
Using **Docker** is the easist and recommended way to launch a Hazel server instantly with minimal configuration. Before you do, you will need [Docker](https://docker.com) installed on your machine. Then, you can pull the [noelware/hazel](https://cr.noelware.cloud/-/noelware/hazel) image.

The image can consist with multiple tags for styles on how to deploy to your environment. We typically build the Hazel images with `linux/amd64` and `linux/arm64` architectures. Windows containers is not planned at the moment.

- `latest`, `nightly` - Uses a specific channel to pull the image from. It is recommended to use the `latest` tag if you wish to use the latest, stable version of Hazel, otherwise, if you want to go in the dark, use the `nightly` tag.

- `alpine` - This tag will use [Alpine](https://hub.docker.com/_/alpine) as the base image due to its low size to make Hazel run on low systems without a lot of system resources.

- `{version}`, `{version}-nightly` - These tags will use a specific version of Hazel to run on. The `-nightly` suffix is for nightly builds, nightly builds are unstable and can lead into bugs.

- `{version}-alpine`, `{version}-nightly-alpine` - These tags will use a pinned version of Hazel, but use [Alpine](https://hub.docker.com/_/alpine) base image instead.

Now, if you wish to use the local filesystem for Hazel, it is recommended to create a Docker volume using the `docker volume` command or with a local path, which can be any path like `~/.local/containers/hazel`.

> **Note**: Hazel only holds persistence over files that are served from the local filesystem. If you wish to use
> other providers, this is not a required step.
>
> You can create a volume with `docker volume create`:
> ```shell
> $ docker volume create hazel
> ```
>
> **Note**: You can substitute `hazel` with any volume name, but you will have to change `hazel` to the volume
> name in later examples if you went with creating a volume with `docker volume`.
>
> For regular filesystem-mounted directories, you will need to change the ownership of the directory so
> the server can read & write to it. You can use the `chown` command to do so:
>
> ```shell
> $ chown -R 1001:1001 <directory>
> ```

Now, we can pull the image from [Noelware's Container Registry](https://cr.noelware.cloud):

```shell
$ docker pull cr.noelware.cloud/hazel/hazel:latest
```

Now, we can run the container!

```shell
# Using -v is an optional step if you're not using the local
# filesystem.
$ docker run -d -p 8989:8989 --name hazel \
    -v hazel:/var/lib/noelware/hazel      \
    -e HAZEL_SERVER_NAME=my-hazel-instance \
    cr.noelware.cloud/hazel/hazel:latest
```

### Helm
Hazel does provide a Helm distribution, but it is not available as of yet!

### Tarball/Zip
Hazel does provide a standard archive to use, but no documentation is available yet.

## Configuration
Hazel uses a standard YAML configuration file that can be used to configure the proxy, as **Hazel** is convention over configuration, it will proxy over the local filesystem under `/var/lib/noelware/hazel` on Linux or `$ROOT/.data` on other operating systems as there is no convention yet.

Hazel supports the local filesystem, Amazon S3, and [MongoDB Gridfs](https://www.mongodb.com/docs/manual/core/gridfs) as Hazel is powered by the [remi-rs](https://github.com/Noelware/remi-rs) library.

### Secure Strings
To prevent leaking sensitive data in configuration files, you can embed environment variables to create a secure string, which will allow you to use the `${}` syntax to load up an environment variable and be used by that, like in Bash.

```yaml
sentry_dsn: ${HAZEL_SENTRY_DSN:-}
```

This will look-up the `HAZEL_SENTRY_DSN` environment variable, if it exists, the result will be the contents of the environment variable, otherwise it'll return "null" (or anything after `:-`).

## Contributing
Thanks for considering contributing to **hazel**! Before you boop your heart out on your keyboard ✧ ─=≡Σ((( つ•̀ω•́)つ, we recommend you to do the following:

- Read the [Code of Conduct](./.github/CODE_OF_CONDUCT.md)
- Read the [Contributing Guide](./.github/CONTRIBUTING.md)

If you read both if you're a new time contributor, now you can do the following:

- [Fork me! ＊*♡( ⁎ᵕᴗᵕ⁎ ）](https://github.com/Noelware/hazel/fork)
- Clone your fork on your machine: `git clone https://github.com/your-username/hazel`
- Create a new branch: `git checkout -b some-branch-name`
- BOOP THAT KEYBOARD!!!! ♡┉ˏ͛ (❛ 〰 ❛)ˊˎ┉♡
- Commit your changes onto your branch: `git commit -am "add features （｡>‿‿<｡ ）"`
- Push it to the fork you created: `git push -u origin some-branch-name`
- Submit a Pull Request and then cry! ｡･ﾟﾟ･(థ Д థ。)･ﾟﾟ･｡

## License
**hazel** is released under the **Apache 2.0** License and with love :purple_heart: by [Noelware](https://noelware.org). :3
