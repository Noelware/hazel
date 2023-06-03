// 🪶 hazel: Minimal, and easy HTTP proxy to map storage provider items into HTTP endpoints
// Copyright 2022-2023 Noelware, LLC. <team@noelware.org>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::{borrow::Cow, str::FromStr};

use aws_sdk_s3::{
    model::{BucketCannedAcl, ObjectCannedAcl},
    Region,
};
use remi_fs::FilesystemStorageConfig;
use remi_s3::S3StorageConfig;
use serde::{Deserialize, Serialize};

use crate::BOOL_REGEX;

use super::{FromEnv, TryFromEnv};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum StorageConfig {
    S3(S3StorageConfig),
    Filesystem(FilesystemStorageConfig),
}

impl Default for StorageConfig {
    fn default() -> Self {
        let data_dir = match std::env::var("HAZEL_STORAGE_FILESYSTEM_DIRECTORY") {
            Ok(val) => val,
            Err(_) => "./data".into(),
        };

        StorageConfig::Filesystem(FilesystemStorageConfig::builder().directory(data_dir).build().unwrap())
    }
}

impl FromEnv<StorageConfig> for StorageConfig {
    fn from_env() -> StorageConfig {
        StorageConfig::try_from_env().unwrap_or_default()
    }
}

impl TryFromEnv<StorageConfig> for StorageConfig {
    fn try_from_env() -> eyre::Result<StorageConfig> {
        match std::env::var("HAZEL_STORAGE_SERVICE") {
            Ok(val) => match val.as_str() {
                "filesystem" | "fs" => {
                    let data_dir = match std::env::var("HAZEL_STORAGE_FILESYSTEM_DIRECTORY") {
                        Ok(val) => val,
                        Err(_) => "./data".into(),
                    };

                    Ok(StorageConfig::Filesystem(
                        FilesystemStorageConfig::builder().directory(data_dir).build().unwrap(),
                    ))
                }

                "s3" => {
                    let enable_signer_v4_requests = match std::env::var("HAZEL_STORAGE_S3_ENABLE_SIGNER_V4_REQUESTS") {
                        Ok(ref val) => BOOL_REGEX.is_match(val),
                        Err(_) => false,
                    };

                    let enforce_path_access_style = match std::env::var("HAZEL_STORAGE_S3_ENFORCE_PATH_ACCESS_STYLE") {
                        Ok(ref val) => BOOL_REGEX.is_match(val),
                        Err(_) => false,
                    };

                    let default_object_acl = match std::env::var("HAZEL_STORAGE_S3_DEFAULT_OBJECT_ACL") {
                        Ok(ref val) => ObjectCannedAcl::from_str(val.as_str())
                            .map_err(|_| eyre!("unable to use '{val}' as default object canned acl"))?,

                        Err(_) => ObjectCannedAcl::PublicRead,
                    };

                    let default_bucket_acl = match std::env::var("HAZEL_STORAGE_S3_DEFAULT_BUCKET_ACL") {
                        Ok(ref val) => BucketCannedAcl::from_str(val.as_str())
                            .map_err(|_| eyre!("unable to use '{val}' as default bucket canned acl"))?,

                        Err(_) => BucketCannedAcl::PublicRead,
                    };

                    let secret_access_key = match std::env::var("HAZEL_STORAGE_S3_SECRET_ACCESS_KEY") {
                        Ok(val) => val,
                        Err(_) => {
                            return Err(eyre!(
                                "Missing required environment variable: `HAZEL_STORAGE_S3_SECRET_ACCESS_KEY`"
                            ))
                        }
                    };

                    let access_key_id = match std::env::var("HAZEL_STORAGE_S3_ACCESS_KEY_ID") {
                        Ok(val) => val,
                        Err(_) => {
                            return Err(eyre!(
                                "Missing required environment variable: `HAZEL_STORAGE_S3_ACCESS_KEY_ID`"
                            ))
                        }
                    };

                    let region = match std::env::var("HAZEL_STORAGE_S3_REGION") {
                        Ok(ref val) => Region::new(Cow::Owned(val.clone())),
                        Err(_) => Region::new(Cow::Owned("us-east-1".to_string())),
                    };

                    let bucket = match std::env::var("HAZEL_STORAGE_S3_BUCKET") {
                        Ok(val) => val,
                        Err(_) => {
                            return Err(eyre!(
                                "Missing required environment variable: `HAZEL_STORAGE_S3_BUCKET`"
                            ))
                        }
                    };

                    let config = S3StorageConfig::builder()
                        .enable_signer_v4_requests(enable_signer_v4_requests)
                        .enforce_path_access_style(enforce_path_access_style)
                        .default_bucket_acl(Some(default_bucket_acl))
                        .default_object_acl(Some(default_object_acl))
                        .secret_access_key(secret_access_key)
                        .access_key_id(access_key_id)
                        .region(Some(region))
                        .bucket(bucket)
                        .build()?;

                    Ok(StorageConfig::S3(config))
                }

                res => Err(eyre!("Unknown storage service to use [{res}]")),
            },

            Err(_) => Err(eyre!("Missing required environment variable: `HAZEL_STORAGE_SERVICE`")),
        }
    }
}
