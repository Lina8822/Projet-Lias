param([string]$JavaHome=$env:JAVA_HOME,[string]$TomcatHome=$env:CATALINA_HOME,[int]$Port=8080)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if(!$JavaHome -or !$TomcatHome){throw 'Définissez JAVA_HOME et CATALINA_HOME.'}
if($Port -lt 1024 -or $Port -gt 65535){throw 'Port invalide.'}
& "$PSScriptRoot/build.ps1" -JavaHome $JavaHome -TomcatHome $TomcatHome
$liasBase=Join-Path $projectRoot 'build/tomcat'
New-Item -ItemType Directory -Force "$liasBase/conf","$liasBase/logs","$liasBase/temp","$liasBase/work","$liasBase/webapps" | Out-Null
if(!(Test-Path "$liasBase/conf/web.xml")){Copy-Item "$TomcatHome/conf/*" "$liasBase/conf" -Force}
# Instance locale indépendante : l'installation Tomcat d'origine n'est pas modifiée.
@"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1"><Service name="Catalina"><Connector address="127.0.0.1" port="$Port" protocol="org.apache.coyote.http11.Http11Nio2Protocol" URIEncoding="UTF-8" connectionTimeout="20000"/><Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false"/></Engine></Service></Server>
"@ | Set-Content "$liasBase/conf/server.xml" -Encoding utf8
Copy-Item "$projectRoot/dist/Lias-Jee.war" "$liasBase/webapps/Lias-Jee.war" -Force
Write-Output "LIAS : http://localhost:$Port/Lias-Jee/ — Ctrl+C pour arrêter."
& "$JavaHome/bin/java.exe" "-Dcatalina.home=$TomcatHome" "-Dcatalina.base=$liasBase" -cp "$TomcatHome/bin/bootstrap.jar;$TomcatHome/bin/tomcat-juli.jar" org.apache.catalina.startup.Bootstrap start
