param([string]$BaseUrl="http://127.0.0.1:18083/Lias-Jee")
$ErrorActionPreference='Stop'
$base=$BaseUrl
if (!$env:LIAS_TEST_PASSWORD -or $env:LIAS_TEST_PASSWORD.Length -lt 12) { throw 'LIAS_TEST_PASSWORD required (12+ characters)' }
$accounts=ConvertFrom-StringData (Get-Content build/test-accounts.properties -Raw)
$results=@()
foreach($route in @('/','/login','/candidature','/equipes','/membres','/publications','/evenements')) {
 $r=Invoke-WebRequest ($base+$route) -SkipHttpErrorCheck
 $results+=[pscustomobject]@{Role='public';Route=$route;Status=$r.StatusCode}
 if($r.StatusCode -ne 200){throw "$route returned $($r.StatusCode)"}
}
foreach($role in @('director','member','doctorant','associe','admin')) {
 $session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
 $r=Invoke-WebRequest ($base+'/LoginServlet') -Method Post -Body @{email=$accounts[$role];password=$env:LIAS_TEST_PASSWORD} -WebSession $session -SkipHttpErrorCheck
 if($r.StatusCode -ne 200 -or $r.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/Lias-Jee/dashboard.jsp'){throw "Login failed $role $($r.StatusCode)"}
 foreach($route in @('/dashboard','/dashboard.jsp','/membres','/publications','/evenements','/demandes','/gouvernance','/profil','/equipes','/documents','/materiel','/conventions','/reunions','/messagerie','/calendrier','/rapport','/notifications','/recherche','/historique')) {
  $r=Invoke-WebRequest ($base+$route) -WebSession $session -SkipHttpErrorCheck
  $results+=[pscustomobject]@{Role=$role;Route=$route;Status=$r.StatusCode}
  $denied = ($route -eq '/demandes' -and $role -ne 'director') -or
    ($route -in @('/gouvernance','/historique') -and $role -notin @('director','admin')) -or
    ($route -eq '/rapport' -and $role -ne 'director') -or
    ($route -in @('/documents','/materiel','/conventions','/reunions','/messagerie') -and $role -in @('doctorant','associe')) -or
    ($route -eq '/calendrier' -and $role -eq 'doctorant')
  $expected=if($denied){403}else{200}
  if($r.StatusCode -ne $expected){throw "$role $route returned $($r.StatusCode)"}
  if($route -like '/dashboard*' -and $r.StatusCode -ne 200){throw "Dashboard failed $role"}
 }
}
$results | ConvertTo-Json | Set-Content build/verification/http-results.json
$results | Format-Table

