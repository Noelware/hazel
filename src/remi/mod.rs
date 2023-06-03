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

use bytes::Bytes;
use remi_core::{Blob, ListBlobsRequest, StorageService, UploadRequest};
use remi_fs::FilesystemStorageService;
use remi_s3::S3StorageService;

use std::{io::Result, path::Path};

use crate::config::{Config, StorageConfig};

// At the moment, remi_rs doesn't provide object-safe trait objects, so we will
// have to delegate over this.
//
// yes, the code will get ugly but I'll fix it later.
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
                StorageServiceDelegate::Filesystem(FilesystemStorageService::new(config.directory()))
            }

            StorageConfig::S3(s3) => StorageServiceDelegate::S3(S3StorageService::new(s3)),
        }
    }
}

#[async_trait]
impl StorageService for StorageServiceDelegate {
    fn name(self) -> &'static str {
        "hazel:remi"
    }

    async fn init(&self) -> Result<()> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.init().await,
            StorageServiceDelegate::S3(s3) => s3.init().await,
        }
    }

    async fn open(&self, path: impl AsRef<Path> + Send) -> Result<Option<Bytes>> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.open(path).await,
            StorageServiceDelegate::S3(s3) => s3.open(path).await,
        }
    }

    async fn blob(&self, path: impl AsRef<Path> + Send) -> Result<Option<Blob>> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.blob(path).await,
            StorageServiceDelegate::S3(s3) => s3.blob(path).await,
        }
    }

    async fn blobs(
        &self,
        path: Option<impl AsRef<Path> + Send>,
        options: Option<ListBlobsRequest>,
    ) -> Result<Vec<Blob>> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.blobs(path, options).await,
            StorageServiceDelegate::S3(s3) => s3.blobs(path, options).await,
        }
    }

    async fn delete(&self, path: impl AsRef<Path> + Send) -> Result<()> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.delete(path).await,
            StorageServiceDelegate::S3(s3) => s3.delete(path).await,
        }
    }

    async fn exists(&self, path: impl AsRef<Path> + Send) -> Result<bool> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.exists(path).await,
            StorageServiceDelegate::S3(s3) => s3.exists(path).await,
        }
    }

    async fn upload(&self, path: impl AsRef<Path> + Send, options: UploadRequest) -> Result<()> {
        match self {
            StorageServiceDelegate::Filesystem(fs) => fs.upload(path, options).await,
            StorageServiceDelegate::S3(s3) => s3.upload(path, options).await,
        }
    }
}
