---
title: Configuration Reference
description: The reference of the `hazel.toml` configuration file.
---

**Hazel** supports loading the configuration via the environment variables either from a `.env` file or set via the host and from a TOML-formatted configuration file that is readable.

<pre>
<a href="#hazel_server_name">server_name</a> = null
<a href="#hazel_sentry_dsn">sentry_dsn</a> = null

[<a href="#hazel_logging">logging</a>]
<a href="#hazel_logging_level">level</a> = "info"
<a href="#hazel_logging_json">json</a> = false

[<a href="#hazel_server">server</a>]
<a href="#hazel_server_port">port</a> = 8989
<a href="#hazel_server_port">host</a> = "0.0.0.0"

[<a href="#hazel_server_ssl">server.ssl</a>]
<a href="#hazel_server_ssl_cert">cert</a> = null
<a href="#hazel_server_sll_cert_key">cert_key</a> = null

[<a href="#hazel_storage_filesystem">storage.filesystem</a>]
<a href="#hazel_storage_filesystem_directory">directory</a> = "./data"

[<a href="#hazel_storage_s3">storage.s3</a>]
<a href="#hazel_storage_s3_enable_signer_v4_requests">enable_signer_v4_requests</a> = false
<a href="#hazel_storage_s3_enforce_path_access_style">enforce_path_access_style</a> = false
<a href="#hazel_storage_s3_default_object_acl">default_object_acl</a> = "bucket-owner-full-control"
<a href="#hazel_storage_s3_default_bucket_acl">default_bucket_acl</a> = "authenticated-read"
<a href="#hazel_storage_s3_secret_access_key">secret_access_key</a> = "{required variable to set}"
<a href="#hazel_storage_s3_access_key_id">access_key_id</a> = "{required variable to set}"
<a href="#hazel_storage_s3_app_name">app_name</a> = "hazel"
<a href="#hazel_storage_s3_endpoint">endpoint</a> = null
<a href="#hazel_storage_s3_prefix">prefix</a> = null
<a href="#hazel_storage_s3_region">region</a> = null
<a href="#hazel_storage_s3_bucket">bucket</a> = "{required variable to set}"

[<a href="#hazel_storage_azure">storage.azure</a>]
<a href="#hazel_storage_azure_container">container</a> = "hazel"
<a href="#hazel_storage_azure_account">account</a> = "{required variable to set}"
</pre>

<a id="hazel_server_name"></a>
## `server_name` (env: `HAZEL_SERVER_NAME`)
- Type: `string`
- Default: `null`

<a id="hazel_sentry_dsn"></a>
## `sentry_dsn` (env: `HAZEL_SENTRY_DSN`)
- Type: `string`
- Default: `null`

<a id="hazel_logging"></a>
## table `logging`

<a id="hazel_logging_level"></a>
### `level` (env: `HAZEL_LOG_LEVEL`)
- Type: `string`
- Possible Values:
    - trace
    - debug
    - info (default)
    - warn
    - error

<a id="hazel_logging_json"></a>
### `json` (env: `HAZEL_LOG_JSON`)
- Type: `boolean`
- Default: `false`

<a id="hazel_server"></a>
## table `server`

<a id="hazel_server_host"></a>
### `host` (env: `HAZEL_SERVER_HOST`, `HOST`)
- Type: `string`
- Default: `0.0.0.0`

<a id="hazel_server_port"></a>
### `port` (env: `HAZEL_SERVER_PORT`, `PORT`)
- Type: `uint16` (0..=65535)
- Default: `8989`

<a id="hazel_server_ssl"></a>
### table `ssl`

<a id="hazel_server_ssl_cert"></a>
#### `cert` (env: `HAZEL_SERVER_SSL_CERT`)
- Type: `path`
- Default: `null`

<a id="hazel_server_ssl_cert_key"></a>
#### `key` (env: `HAZEL_SERVER_SSL_CERT_KEY`)
- Type: `path`
- Default: `null`
