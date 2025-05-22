<div align="center">
    <h3>🪶 <code>hazel</code></h3>
    <h4>Easy to use read-only proxy to map objects to URLs</h4>
    <hr />
</div>

Hazel is Noelware's microservice to proxy our objects that we publish (like `artifacts.noelware.org` for all binary artifacts) to URLs that are mapped by their object storage location.

**Hazel** was originally maintained only by [Noel Towa](https://floofy.dev) but now is maintained and controlled by the Noelware team.

## Installation
### Install Script
> [!IMPORTANT]
> Please install the scripts first and audit them before running.

```shell
# Unix:
$ curl -fsSL https://noelware.org/x/hazel | sh -

# Windows:
$ irm https://noelware.org/x/hazel.ps1 | iex
```

### Docker
Running Hazel as a Docker container is the most recommended way to run the Hazel server with minimal configuration for smaller deployments. You can pull the `docker.noelware.org/noelware/hazel` image to run the Hazel server.

The image can consist with multiple tags for styles on how to deploy to your environment. We typically build the Hazel images with `linux/amd64` and `linux/arm64` architectures. Windows containers is not planned at the moment.

- `latest` - Uses a specific channel to pull the image from. `latest` will be the latest stable version, `beta` will be the latest beta version.

- `alpine` - This tag will use [Alpine](https://hub.docker.com/_/alpine) as the base image due to its low size to make Hazel run on low systems without a lot of system resources.

> [!NOTE]
> Hazel only holds persistence over files that are served from the local filesystem. If you wish to use
> other providers, this is not a required step.
>
> You can create a volume with `docker volume create`:
> ```shell
> $ docker volume create hazel
> ```
>
> You can substitute `hazel` with any volume name, but you will have to change `hazel` to the volume
> name in later examples if you went with creating a volume with `docker volume`.
>
> For regular filesystem-mounted directories, you will need to change the ownership of the directory so
> the server can read and write to it. You can use the `chown` command to do so:
>
> ```shell
> $ chown -R 1001:1001 <directory>
> ```

Now, we can pull the image from [Noelware's Container Registry](https://cr.noelware.cloud):

```shell
$ docker pull cr.noelware.cloud/noelware/hazel
```

Now, we can run the container!

```shell
# Using -v is an optional step if you're not using the local filesystem.
$ docker run -d -p 8989:8989 --name hazel \
    -e HAZEL_SERVER_NAME=my-hazel-instance \
    -v /var/lib/noelware/hazel/data:my-volume \
    cr.noelware.cloud/noelware/hazel
```

### Helm Chart
Refer to the [`Noelware/helm-charts`](https://github.com/Noelware/helm-charts/tree/master/charts/noelware/hazel) repository for more information.

### NixOS
On a NixOS machine, you can use the [`nixpkgs-noelware`] overlay to install a Hazel server on a NixOS server:

```nix
{
    services.hazel.enable = true;
}
```

[`nixpkgs-noelware`]: https://github.com/Noelware/nixpkgs-noelware

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
Hazel is released under the **Apache 2.0** License and with love :purple_heart: by [Noelware](https://noelware.org). :3
