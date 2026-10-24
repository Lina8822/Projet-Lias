param([Parameter(Mandatory=$true)][string]$Stage)
$ErrorActionPreference='Stop'
$env:JAVA_HOME='C:/Program Files/Java/jdk-26.0.1'
& "$PSScriptRoot/../build/verification/apache-maven-3.9.11/bin/mvn.cmd" '-Dmaven.repo.local=build/verification/m2' clean test package *> "build/final-verification/maven-$Stage.log"
if($LASTEXITCODE -ne 0){Get-Content "build/final-verification/maven-$Stage.log" -Tail 30;throw 'Build failed'}
Get-Content target/surefire-reports/com.lias.LaunchRegressionTest.txt
Write-Output "BUILD SUCCESS - $Stage"
