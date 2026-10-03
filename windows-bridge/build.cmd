@echo off
setlocal

set CSC=C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe
set OUTPUT=%~dp0build

if not exist "%CSC%" (
  echo Required compiler was not found: %CSC%
  exit /b 1
)

if not exist "%OUTPUT%" mkdir "%OUTPUT%"

"%CSC%" /nologo /target:exe /platform:anycpu /optimize+ ^
  /reference:System.dll ^
  /reference:System.Core.dll ^
  /reference:System.Configuration.dll ^
  /reference:System.ServiceModel.dll ^
  /reference:System.Runtime.Serialization.dll ^
  /reference:System.Web.Extensions.dll ^
  /out:"%OUTPUT%\SuPOS.WindowsBridge.exe" ^
  "%~dp0src\Program.cs" ^
  "%~dp0src\BridgeSettings.cs" ^
  "%~dp0src\Contracts.cs" ^
  "%~dp0src\PtcTradeReader.cs" ^
  "%~dp0src\BridgeHost.cs" ^
  "%~dp0src\BridgeSelfTest.cs"

if errorlevel 1 exit /b 1

copy /y "%~dp0App.config" "%OUTPUT%\SuPOS.WindowsBridge.exe.config" >nul
echo Built %OUTPUT%\SuPOS.WindowsBridge.exe
