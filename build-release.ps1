param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidHome = $env:ANDROID_HOME,
    [string]$GradleCommand = ""
)

$ErrorActionPreference = "Stop"

$Project = $PSScriptRoot
$Parent = Split-Path -Parent $Project
$LocalToolchain = Join-Path $Parent ".toolchain"

if (-not $JavaHome) {
    $Candidate = Join-Path $LocalToolchain "jdk"
    if (Test-Path -LiteralPath (Join-Path $Candidate "bin\java.exe")) {
        $JavaHome = $Candidate
    }
}
if (-not $AndroidHome) {
    $Candidate = Join-Path $LocalToolchain "android-sdk"
    if (Test-Path -LiteralPath $Candidate) {
        $AndroidHome = $Candidate
    }
}
if (-not $JavaHome) {
    throw "JAVA_HOME is required. Install JDK 17 or pass -JavaHome."
}
if (-not $AndroidHome) {
    throw "ANDROID_HOME is required. Install Android SDK 35 or pass -AndroidHome."
}

if (-not $GradleCommand) {
    $LocalGradle = Join-Path $LocalToolchain "dl\gradle-8.9-mirror\gradle-8.9\bin\gradle.bat"
    $GradleCommand = if (Test-Path -LiteralPath $LocalGradle) {
        $LocalGradle
    } else {
        Join-Path $Project "gradlew.bat"
    }
}

$BuildRoot = $Project
if ($Project -match "[^\x00-\x7F]") {
    $BuildRoot = Join-Path $env:TEMP "shiying-android-ascii-build"
    if (-not (Test-Path -LiteralPath $BuildRoot)) {
        New-Item -ItemType Junction -Path $BuildRoot -Target $Project | Out-Null
    }
    $BuildLink = Get-Item -LiteralPath $BuildRoot
    if ($BuildLink.LinkType -ne "Junction") {
        throw "ASCII build path exists but is not a junction: $BuildRoot"
    }
}

$StoreFile = Join-Path $Project "shiying.keystore"
$PropertiesFile = Join-Path $Project "keystore.properties"
$StorePassword = $env:SHIYING_STORE_PASSWORD
$KeyPassword = $env:SHIYING_KEY_PASSWORD
$KeyAlias = if ($env:SHIYING_KEY_ALIAS) { $env:SHIYING_KEY_ALIAS } else { "shiying" }

if (-not (Test-Path -LiteralPath $StoreFile)) {
    if (-not $StorePassword) {
        $StorePassword = [Guid]::NewGuid().ToString("N")
    }
    if (-not $KeyPassword) {
        $KeyPassword = $StorePassword
    }
    & (Join-Path $JavaHome "bin\keytool.exe") `
        -genkeypair `
        -keystore $StoreFile `
        -storepass $StorePassword `
        -keypass $KeyPassword `
        -alias $KeyAlias `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname "CN=ShiYing Android, OU=Open Source, O=ShiYing, C=CN"
}

if (-not (Test-Path -LiteralPath $PropertiesFile)) {
    if (-not $StorePassword -or -not $KeyPassword) {
        throw "Existing keystore requires SHIYING_STORE_PASSWORD and SHIYING_KEY_PASSWORD."
    }
    @"
storeFile=shiying.keystore
storePassword=$StorePassword
keyAlias=$KeyAlias
keyPassword=$KeyPassword
"@ | Set-Content -Encoding ASCII -Path $PropertiesFile
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
Set-Location $BuildRoot

& $GradleCommand testDebugUnitTest --rerun-tasks
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed" }

& $GradleCommand assembleRelease
if ($LASTEXITCODE -ne 0) { throw "Release build failed" }

New-Item -ItemType Directory -Force -Path (Join-Path $Project "dist") | Out-Null
$Release = Join-Path $Project "app\build\outputs\apk\release\app-release.apk"
$Output = Join-Path $Project "dist\ShiYingAndroid-arm64.apk"
Copy-Item -LiteralPath $Release -Destination $Output -Force

$Signer = Join-Path $AndroidHome "build-tools\34.0.0\apksigner.bat"
& $Signer verify --verbose $Output
if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed" }

Write-Host "APK ready: $Output"
