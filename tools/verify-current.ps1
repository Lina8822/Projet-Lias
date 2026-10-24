param([string]$BaseUrl='http://127.0.0.1:18080/Lias-Jee/',[string]$Database='lias_test_final_20261005',[int]$DatabasePort=33307,[string]$Python='python')
$ErrorActionPreference='Stop'
if (!$Database.StartsWith('lias_test')) { throw 'La recette automatique exige une base isolée lias_test*.' }
Set-Location (Split-Path $PSScriptRoot -Parent)
$env:LIAS_TEST_BASE=$BaseUrl
$env:LIAS_TEST_DB=$Database
$env:LIAS_TEST_DB_PORT=[string]$DatabasePort
$env:LIAS_TEST_OUT='build/current-verification'
$env:PYTHONIOENCODING='utf-8'
New-Item -ItemType Directory -Force $env:LIAS_TEST_OUT | Out-Null
foreach ($liasScenario in @('matrix_http','final_mutations','lifecycle_http','adhesion_http','publications_http','communication_http','material_member_http','search_profiles_http','crossprofile_http','personal_dates_http','governance_http','succession_http','history_audit_http','collaboration_http','report_pdf_http','verify_report_pdf','final_security','final_profile_documents','extended_http')) {
    & $Python "$PSScriptRoot/$liasScenario.py" *> "$env:LIAS_TEST_OUT/$liasScenario.log"
    if ($LASTEXITCODE -ne 0) { Get-Content "$env:LIAS_TEST_OUT/$liasScenario.log" -Tail 15; throw "Scénario en échec : $liasScenario" }
    Write-Output "PASS $liasScenario"
}
