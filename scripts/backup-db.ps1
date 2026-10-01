<#
.SYNOPSIS
    Backs up the flowershop PostgreSQL database to a timestamped custom-format dump file.

.EXAMPLE
    .\backup-db.ps1
    .\backup-db.ps1 -OutputDir "D:\backups"
#>
param(
    [string]$OutputDir = "$PSScriptRoot\..\backups"
)

$PgBin = "C:\Program Files\PostgreSQL\18\bin"
$DbName = "flowershop"
$DbUser = "postgres"

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputFile = Join-Path $OutputDir "flowershop_$timestamp.backup"

Write-Host "Backing up database '$DbName' to $outputFile"
if (-not $env:PGPASSWORD) {
    Write-Host "(You will be prompted for the postgres password unless `$env:PGPASSWORD is set.)"
}

& "$PgBin\pg_dump.exe" -U $DbUser -h localhost -d $DbName -F c -f $outputFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "Backup completed: $outputFile" -ForegroundColor Green
} else {
    Write-Host "Backup failed (exit code $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}
