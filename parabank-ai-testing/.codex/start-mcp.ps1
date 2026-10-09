$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$repository = Join-Path $projectRoot '.maven-cache\repository'
$runtimeArtifacts = @(
  'com\microsoft\playwright\playwright\*\playwright-*.jar',
  'com\microsoft\playwright\driver\*\driver-*.jar',
  'com\microsoft\playwright\driver-bundle\*\driver-bundle-*.jar',
  'com\google\code\gson\gson\*\gson-*.jar',
  'com\fasterxml\jackson\core\jackson-annotations\*\jackson-annotations-*.jar',
  'com\fasterxml\jackson\core\jackson-core\*\jackson-core-*.jar',
  'com\fasterxml\jackson\core\jackson-databind\*\jackson-databind-*.jar',
  'org\slf4j\slf4j-api\*\slf4j-api-*.jar'
)
$classpathEntries = @((Join-Path $projectRoot 'target\classes'))
foreach ($artifact in $runtimeArtifacts) {
  $matches = @(Get-ChildItem -Path (Join-Path $repository $artifact) -File -ErrorAction Stop)
  if ($matches.Count -ne 1) { throw "Expected one runtime dependency for $artifact; found $($matches.Count)." }
  $classpathEntries += $matches[0].FullName
}
# If unset, browser_open locks the run to the exact host of the URL supplied by the user.
Remove-Item Env:BROWSER_ALLOWED_HOSTS -ErrorAction SilentlyContinue
& java -cp ($classpathEntries -join [IO.Path]::PathSeparator) com.parabank.mcp.PlaywrightMcpServer
exit $LASTEXITCODE
