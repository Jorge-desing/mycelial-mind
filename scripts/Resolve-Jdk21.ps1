<#
.SYNOPSIS
    Resolves JDK 21 and sets JAVA_HOME and GRADLE_OPTS consistently across scripts.
#>
$preferredJavaHome = 'C:\Program Files\Java\jdk-21'
if (-not (Test-Path -LiteralPath $preferredJavaHome)) {
    $preferredJavaHome = 'C:\Program Files\Java\graalvm-jdk-21.0.7+8.1'
}
if (Test-Path -LiteralPath $preferredJavaHome) {
    $env:JAVA_HOME = $preferredJavaHome
}
$env:GRADLE_OPTS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
$script:ResolvedJavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { $preferredJavaHome }
