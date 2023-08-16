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

use crate::config::{Config, StorageConfig};
use remi_fs::{ContentTypeResolver, DefaultContentTypeResolver, FilesystemStorageService};
use remi_s3::S3StorageService;
use std::io::Result;

#[derive(Debug, Clone, Copy)]
pub struct HazelContentTypeResolver {
    default_resolver: DefaultContentTypeResolver,
}

impl ContentTypeResolver for HazelContentTypeResolver {
    fn resolve(&self, bytes: &[u8]) -> String {
        // TODO: we can't detect any other serde_* formats as we'll
        // bloat up hazel, so we are only doing what we have.
        //
        // TODO(@Noelware): try to do the same with remi_fs?
        match serde_json::from_slice::<serde_json::Value>(bytes) {
            Ok(_) => "application/json".into(),
            Err(_) => match serde_yaml::from_slice::<serde_yaml::Value>(bytes) {
                Ok(_) => "text/yaml".into(),
                Err(_) => self.default_resolver.resolve(bytes),
            },
        }
    }
}

// TODO(@auguwu): switch to dyn StorageService once remi_core::StorageService
// is trait object-safe.
#[derive(Debug, Clone)]
pub enum StorageServiceDelegate {
    Filesystem(FilesystemStorageService),
    S3(S3StorageService),
}

impl Default for StorageServiceDelegate {
    fn default() -> Self {
        let config = Config::get();
        match config.storage.clone() {
            StorageConfig::Filesystem(config) => {
                let mut service = FilesystemStorageService::new(config.directory());
                service.set_content_type_resolver(HazelContentTypeResolver {
                    default_resolver: DefaultContentTypeResolver,
                });

                StorageServiceDelegate::Filesystem(service.clone())
            }

            StorageConfig::S3(s3) => StorageServiceDelegate::S3(S3StorageService::new(s3)),
        }
    }
}

macro_rules! gen_impl {
    (
        $(
            fn $name:ident($($arg:ident: $ty:ty),*) -> $return_:ty;
        )*
    ) => {
        impl ::remi_core::StorageService for StorageServiceDelegate {
            fn name(self) -> &'static str {
                "hazel:remi"
            }

            $(
                fn $name<'life0, 'async_trait>(
                    &'life0 self,
                    $($arg: $ty,)*
                ) -> ::std::pin::Pin<::std::boxed::Box<dyn ::std::future::Future<Output = $return_> + Send + 'async_trait>>
                where
                    'life0: 'async_trait,
                    Self: 'async_trait,
                {
                    Box::pin(async move {
                        let __self = self;
                        let __code: $return_ = {
                            match self {
                                StorageServiceDelegate::Filesystem(fs) => fs.$name($($arg),*).await,
                                StorageServiceDelegate::S3(s3) => s3.$name($($arg),*).await
                            }
                        };

                        __code
                    })
                }
            )*
        }
    };
}

gen_impl! {
    fn init() -> Result<()>;
    fn open(path: impl AsRef<::std::path::Path> + Send + 'async_trait) -> Result<Option<::bytes::Bytes>>;
    fn blob(path: impl AsRef<::std::path::Path> + Send + 'async_trait) -> Result<Option<::remi_core::Blob>>;
    fn blobs(path: Option<impl AsRef<::std::path::Path> + Send + 'async_trait>, options: Option<::remi_core::ListBlobsRequest>) -> Result<Vec<remi_core::Blob>>;
    fn delete(path: impl AsRef<::std::path::Path> + Send + 'async_trait) -> Result<()>;
    fn exists(path: impl AsRef<::std::path::Path> + Send + 'async_trait) -> Result<bool>;
    fn upload(path: impl AsRef<::std::path::Path> + Send + 'async_trait, options: ::remi_core::UploadRequest) -> Result<()>;
}
