# Prepare + build the all-in-one Windows installer for CharlzTechTV 1.0.11
# Bundles: full JRE, VLC natives, VC++ redistributable

$ErrorActionPreference = "Stop"
$Packaging = $PSScriptRoot
$Desktop = Split-Path -Parent $Packaging
$Root = Split-Path -Parent $Desktop
$VlcDest = Join-Path $Packaging "resources\windows"
$Redist = Join-Path $Packaging "redist\vc_redist.x64.exe"
$Iss = Join-Path $Packaging "CharlzTechTV.iss"
$AppDir = Join-Path $Desktop "build\compose\binaries\main\app\CharlzTechTV"
$OutDir = Join-Path $Desktop "build\compose\binaries\main\installer"

function Ensure-VlcBundle {
    $lib = Join-Path $VlcDest "libvlc.dll"
    if (Test-Path $lib) {
        Write-Host "VLC bundle OK: $VlcDest"
        return
    }
    $vlc = Join-Path ${env:ProgramFiles} "VideoLAN\VLC"
    if (-not (Test-Path (Join-Path $vlc "libvlc.dll"))) {
        throw "VLC not found at '$vlc'. Install VLC x64 once, or place natives in desktop\packaging\resources\windows\"
    }
    New-Item -ItemType Directory -Force -Path $VlcDest | Out-Null
    Get-ChildItem $vlc -File -Filter *.dll |
        Where-Object { $_.Name -notin @("axvlc.dll", "npvlc.dll") } |
        ForEach-Object { Copy-Item $_.FullName $VlcDest -Force }
    Copy-Item (Join-Path $vlc "plugins") (Join-Path $VlcDest "plugins") -Recurse -Force
    Write-Host "Copied VLC natives into $VlcDest"
}

function Ensure-VCRedist {
    New-Item -ItemType Directory -Force -Path (Split-Path $Redist) | Out-Null
    if (-not (Test-Path $Redist)) {
        Write-Host "Downloading Visual C++ Redistributable..."
        Invoke-WebRequest -Uri "https://aka.ms/vs/17/release/vc_redist.x64.exe" -OutFile $Redist -UseBasicParsing
    }
    Write-Host "VC++ redist OK: $Redist"
}

function Find-ISCC {
    $candidates = @(
        "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
        "${env:ProgramFiles}\Inno Setup 6\ISCC.exe",
        "${env:LocalAppData}\Programs\Inno Setup 6\ISCC.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    $cmd = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw "Inno Setup (ISCC.exe) not found. Install with: winget install JRSoftware.InnoSetup"
}

Write-Host "Root=$Root"
Write-Host "Desktop=$Desktop"
Write-Host "Packaging=$Packaging"
Write-Host "== Preparing dependencies =="
Ensure-VlcBundle
Ensure-VCRedist

Write-Host "== Building distributable (full JRE + VLC resources) =="
Push-Location $Root
try {
    & .\gradlew.bat ":desktop:clean" ":desktop:createDistributable" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle createDistributable failed ($LASTEXITCODE)" }
} finally {
    Pop-Location
}

if (-not (Test-Path (Join-Path $AppDir "CharlzTechTV.exe"))) {
    throw "Missing packaged app at $AppDir"
}
$bundledVlc = Join-Path $AppDir "app\resources\libvlc.dll"
if (-not (Test-Path $bundledVlc)) {
    throw "Bundled VLC missing from packaged app (expected $bundledVlc)"
}
Write-Host "Bundled VLC OK: $bundledVlc"

Write-Host "== Compiling Inno Setup installer =="
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$iscc = Find-ISCC
& $iscc `
    "/DAppSource=$AppDir" `
    "/DRedistSource=$Redist" `
    "/DOutputDir=$OutDir" `
    $Iss
if ($LASTEXITCODE -ne 0) { throw "ISCC failed ($LASTEXITCODE)" }

$setup = Get-ChildItem $OutDir -Filter "CharlzTechTV-Setup-*.exe" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Write-Host ""
Write-Host "Installer ready:"
Write-Host "  $($setup.FullName)"
Write-Host "  Size: $([math]::Round($setup.Length/1MB,1)) MB"
