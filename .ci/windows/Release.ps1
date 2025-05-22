# 🪶 Hazel: Easy to use read-only proxy to map objects to URLs
# Copyright 2022-2025 Noelware, LLC. <team@noelware.org>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

[CmdletBinding()]
Param(
    [String]$Cargo = "cargo",
    [String]$CargoFlags = "",
    [String]$BuildFlags = ""
)

$ErrorActionPreference = "Stop"
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

. "$PSScriptRoot\..\_shared.ps1"

function Main {
    if (![System.Environment]::Is64BitOperatingSystem) {
        Write-Error "Hazel is not supported on 32-bit systems"
        Exit 1
    }

    StartGroup "Build / Windows (x64)"

    $ResultDir = $(New-Item -Path . -Name ".result" -ItemType Directory).FullName

    Write-Host ">> $ $Cargo $CargoFlags build --release --locked --target x86_64-pc-windows-msvc --bin hazel $BuildFlags"
    Invoke-Expression "$Cargo $CargoFlags build --release --locked --target x86_64-pc-windows-msvc --bin hazel $BuildFlags"

    # don't attempt to continue if Cargo failed to build Hazel
    if ($LASTEXITCODE -ne 0) {
        Exit $LASTEXITCODE
    }

    Write-Host "$ mv ./target/x86_64-pc-windows-msvc/release/hazel.exe $ResultDir/hazel-windows-x86_64.exe"
    Move-Item -Path "./target/x86_64-pc-windows-msvc/release/hazel.exe" "$ResultDir/hazel-windows-x86_64.exe"

    Push-Location "$ResultDir"

    Write-Host "Generating sha256sum: hazel-windows-x86_64.exe"

    $Hash = (Get-FileHash -Path hazel-windows-x86_64.exe).Hash.ToLower()
    Write-Output "$Hash  hazel-windows-x86_64.exe" | Out-File hazel-windows-x86_64.exe.sha256

    Pop-Location

    Write-Host "Finished! All resources will be in $ResultDir ~!"
    EndGroup
}

Main
