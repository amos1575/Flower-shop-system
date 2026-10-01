<#
.SYNOPSIS
    Restores the flowershop PostgreSQL database from a custom-format dump file
    created by backup-db.ps1. Existing objects are dropped and recreated.

.EXAMPLE
    .\restore-db.ps1 -BackupFile "..\backups\flowershop_20260708_200000.backup"
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
)

$PgBin = "C:\Program Files\PostgreSQL\18\bin"
$DbName = "flowershop"
$DbUser = "postgres"

if (-not (Test-Path $BackupFile)) {
    Write-Host "Backup file not found: $BackupFile" -ForegroundColor Red
    exit 1
}

Write-Host "This will DROP and recreate existing objects in database '$DbName' using '$BackupFile'." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to cancel, or Enter to continue."
Read-Host | Out-Null

if (-not $env:PGPASSWORD) {
    Write-Host "(You will be prompted for the postgres password unless `$env:PGPASSWORD is set.)"
}

& "$PgBin\pg_restore.exe" -U $DbUser -h localhost -d $DbName --clean --if-exists -v $BackupFile

Write-Host "Restore finished (exit code $LASTEXITCODE) - review the output above for any warnings." -ForegroundColor Cyan
