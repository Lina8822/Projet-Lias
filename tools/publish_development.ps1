$ErrorActionPreference='Stop'
$actualProject='C:/Users/ELECTRO-RIMO/Downloads/Lias-Jee'
$deployment='C:/Users/ELECTRO-RIMO/eclipse-workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/Lias-Jee'
if(!(Test-Path "$actualProject/.project") -or !(Test-Path "$deployment/WEB-INF")){throw 'Unexpected development paths'}
foreach($folder in @('src','docs','tools','database','target')){Copy-Item "$folder/*" "$actualProject/$folder" -Recurse -Force}
Copy-Item 'target/Lias-Jee/*' $deployment -Recurse -Force
& 'C:/Program Files/Java/jdk-26.0.1/bin/java.exe' --add-modules jdk.attach tools/ReloadDevelopment.java
if($LASTEXITCODE -ne 0){throw 'Reload failed'}
