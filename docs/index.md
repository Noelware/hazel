---
title: 🪶 ヘイゼル hazel
description: Minimal, simple, and open source content delivery network made in Kotlin
---

# 🪶 hazel
**hazel** (case-sensitive) is a minimal, simple, and open source content delivery network made in Kotlin. It contains features
as manipulation of images, resizing images, and much more. This was built since my CDNs (cdn.floofy.dev, cdn.noelware.org) are moving
away from Wasabi (into MinIO), and doing a CNAME would make it more complex, so **hazel** was built.

I could've used other CDN networks, right? I wanted to build my own twist, so... **hazel** was built!

## Supported Providers
- Using the local **filesystem**,
- Using **Amazon S3** or an Amazon S3 compatible service

## Installation
You can install **hazel** from:

- Docker using the [Docker image](#docker);
- the [Helm Chart](#helm-chart) hosted on `charts.noelware.org`;
- [GitHub Releases](#releases);
- [Homebrew](#homebrew), [Scoop](#scoop-bucket)

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

### Homebrew
soon:tm:

### Scoop Bucket
soon:tm:

### GitHub Releases
You can install the .zip or .tar.gz file (depending on the operating system) from cURL or with [eget](https://github.com/zyedidia/eget).

#### eget
```shell
$ eget auguwu/hazel
```

#### cURL
```shell
$ curl -XGET -L -o /tmp/hazel.tar.gz https://github.com/auguwu/hazel/releases/download/{{VERSION}}/hazel-linux-amd64-{{VERSION}}-glib.tar.gz
```

##### Binary Specification
- `hazel-{OS}-{ARCH}-{VERSION}[-{COMPILER}]`

- `{OS}` being the host operating system, i.e, `linux`/`windows`/`macos`
- `{ARCH}` being the host architecture, i.e, `amd64`/`arm64`
- `{VERSION}` being the version of the CLI, i.e, 1.0.0
- `{COMPILER}` being the compiler, i.e, `glibc`/`musl`
