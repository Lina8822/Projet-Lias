param([string]$JavaHome=$env:JAVA_HOME,[string]$TomcatHome=$env:CATALINA_HOME)
$ErrorActionPreference='Stop'
& "$PSScriptRoot/build.ps1" -JavaHome $JavaHome -TomcatHome $TomcatHome
& "$JavaHome/bin/javac.exe" -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes src/test/java/com/lias/IntegrationTest.java
if($LASTEXITCODE -ne 0){throw 'Compilation tests en échec'}
& "$JavaHome/bin/java.exe" '-DLIAS_DB_URL=jdbc:mysql://127.0.0.1:33307/lias_test?connectionTimeZone=UTC' -cp 'build/classes;build/test-classes;src/main/webapp/WEB-INF/lib/mysql-connector-j-9.7.0.jar' com.lias.IntegrationTest > build/integration-results.txt 2>&1
if($LASTEXITCODE -ne 0){Get-Content build/integration-results.txt -Tail 15;throw 'Tests métier en échec'}
Get-Content build/integration-results.txt -Tail 6
