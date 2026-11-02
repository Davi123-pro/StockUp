# Salva uma copia do codigo-fonte (sem build/, sem APK) em _backups/
# Uso:  pwsh -File ferramentas\salvar-codigo.ps1
$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path $raiz "_backups"
New-Item -ItemType Directory -Force -Path $destino | Out-Null
$carimbo = Get-Date -Format "yyyyMMdd-HHmm"
$zip = Join-Path $destino "stockup-codigo-$carimbo.zip"

$itens = @(
  "app\src", "app\build.gradle.kts", "app\proguard-rules.pro",
  "build.gradle.kts", "settings.gradle.kts", "gradle.properties",
  "gradle\libs.versions.toml", "gradle\wrapper\gradle-wrapper.properties",
  "VISAO_PRODUTO.md"
) | Where-Object { Test-Path (Join-Path $raiz $_) } | ForEach-Object { Join-Path $raiz $_ }

Compress-Archive -Path $itens -DestinationPath $zip -CompressionLevel Optimal -Force
$info = Get-Item $zip
Write-Host ("Salvo: {0} ({1} KB)" -f $info.Name, [math]::Round($info.Length/1KB,0)) -ForegroundColor Green

# Mantem no maximo 20 snapshots, apagando os mais antigos
$todos = Get-ChildItem $destino -Filter "stockup-codigo-*.zip" | Sort-Object LastWriteTime -Descending
if ($todos.Count -gt 20) { $todos | Select-Object -Skip 20 | Remove-Item -Force }
