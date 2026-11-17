param([string]$SdkRoot=$env:ANDROID_SDK_ROOT,[string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if (!$SdkRoot -or !$JavaHome) { throw 'Set ANDROID_SDK_ROOT and JAVA_HOME, or pass -SdkRoot and -JavaHome.' }
$env:JAVA_HOME=$JavaHome
$env:PATH=(Join-Path $JavaHome 'bin')+';'+$env:PATH
$bt=Join-Path $SdkRoot 'build-tools/35.0.0'
$platform=Join-Path $SdkRoot 'platforms/android-35/android.jar'
$out=Join-Path $PSScriptRoot 'app/build/manual'
foreach($dir in @('classes','tests','dex','apk')) { New-Item -ItemType Directory -Force (Join-Path $out $dir) | Out-Null }
function Run([string]$program,[string[]]$arguments) { & $program @arguments; if($LASTEXITCODE -ne 0) { throw "$program failed ($LASTEXITCODE)" } }
$src='app/src/main/java/ru/tileclock'
$testSources=@("$src/TurnTimer.java","$src/GameLayoutPlan.java","$src/SetupLayoutPlan.java","$src/PlayerOrder.java")+@(Get-ChildItem tests/*.java | ForEach-Object FullName)
Run 'javac' (@('-encoding','UTF-8','-d',"$out/tests")+$testSources)
foreach($test in @('TurnTimerTest','GameLayoutTest','SetupLayoutTest','PlayerOrderTest')) { Run 'java' @('-cp',"$out/tests",$test) }
Run 'javac' (@('--release','17','-encoding','UTF-8','-classpath',$platform,'-d',"$out/classes")+@(Get-ChildItem "$src/*.java" | ForEach-Object FullName))
Run 'jar' @('--create','--file',"$out/classes.jar",'-C',"$out/classes",'.')
Run "$bt/d8.bat" @('--lib',$platform,'--min-api','26','--output',"$out/dex","$out/classes.jar")
Run "$bt/aapt2.exe" @('compile','--dir','app/src/main/res','-o',"$out/resources.zip")
$utf8=New-Object Text.UTF8Encoding($false)
$manifest=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'app/src/main/AndroidManifest.xml')).Replace('<manifest ','<manifest package="ru.tileclock" ')
[IO.File]::WriteAllText("$out/AndroidManifest.xml",$manifest,$utf8)
$gradle=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'app/build.gradle'))
$versionCode=[regex]::Match($gradle,'versionCode\s+(\d+)').Groups[1].Value
$versionName=[regex]::Match($gradle,"versionName\s+'([^']+)'").Groups[1].Value
Run "$bt/aapt2.exe" @('link','-o',"$out/apk/unsigned.apk",'--manifest',"$out/AndroidManifest.xml",'-I',$platform,'--min-sdk-version','26','--target-sdk-version','35','--version-code',$versionCode,'--version-name',$versionName,"$out/resources.zip")
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$zip=[IO.Compression.ZipFile]::Open("$out/apk/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try { foreach($dex in Get-ChildItem "$out/dex/*.dex") { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$dex.FullName,$dex.Name) | Out-Null } } finally { $zip.Dispose() }
Run "$bt/zipalign.exe" @('-f','-p','4',"$out/apk/unsigned.apk","$out/apk/aligned.apk")
$key=Join-Path $PSScriptRoot '.local-signing/development.jks'
if(!(Test-Path $key)) {
 New-Item -ItemType Directory -Force (Split-Path $key) | Out-Null
 Run 'keytool' @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=TileClock Development,O=TileClock,C=RU')
}
$apk="$out/apk/rummikub-timer-v$versionName.apk"
Run "$bt/apksigner.bat" @('sign','--ks',$key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$out/apk/aligned.apk")
Run "$bt/apksigner.bat" @('verify','--verbose',$apk)
Run "$bt/aapt2.exe" @('dump','badging',$apk)
Write-Output "APK: $apk"
