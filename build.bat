@echo off
setlocal enabledelayedexpansion
title Nixa Toolkit - Build (Java)

echo ============================================
echo  Nixa Toolkit - Java Build
echo ============================================
echo.

rem --- Step 0: make sure a real JDK (not just a JRE) is on PATH -------------
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] "javac" command kidaikala.
    echo.
    echo Idhu build panna, unga computer-la JDK ^(Java Development Kit^) version 17
    echo alladhu adhukku mela install pannirukanum. JRE mattum podhadhu - javac,
    echo jar, jpackage ithellam JDK-la dhan varum.
    echo.
    echo Free JDK download pannurathukku ^(Eclipse Temurin, Adoptium^):
    echo   https://adoptium.net
    echo.
    echo Install pannina apparam, ippo terminal-ah close pannitu, thirumba open panni
    echo indha build.bat-ah run pannunga.
    echo.
    pause
    exit /b 1
)

for /f "tokens=2 delims=." %%v in ('javac -version 2^>^&1') do set JAVAC_VER=%%v
echo Using javac:
javac -version
echo.

where jpackage >nul 2>nul
if errorlevel 1 (
    echo [ERROR] "jpackage" command kidaikala.
    echo.
    echo Unga JDK romba pazhaiyadhu ^(old^) irukalam - jpackage-ku JDK 14 alladhu adhukku
    echo mela venum. https://adoptium.net -la irundhu pudhusa oru JDK ^(17 LTS
    echo alladhu adhukku mela^) install pannunga.
    echo.
    pause
    exit /b 1
)

rem --- Step 1: clean + compile ------------------------------------------------
echo [1/4] Compiling Java source...
if exist out rmdir /s /q out
mkdir out

dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo [ERROR] Compile fail aachu. Mேlே irukka error message-ah paarunga.
    pause
    exit /b 1
)
echo     OK - compiled cleanly.
echo.

rem --- Step 2: runnable jar ----------------------------------------------------
echo [2/4] Building NixaToolkit.jar...
if exist dist rmdir /s /q dist
mkdir dist
pushd out
jar --create --file ..\dist\NixaToolkit.jar --main-class com.nixatoolkit.App com
popd
if errorlevel 1 (
    echo [ERROR] jar build fail aachu.
    pause
    exit /b 1
)
echo     OK - dist\NixaToolkit.jar ready.
echo.

rem --- Step 3: jpackage - standalone Windows app (bundled Java runtime) -------
echo [3/4] Building a standalone Windows app with jpackage...
echo       (Idhula thani Java runtime bundle aagum - vera Java venam intha PC-la
echo        run panna. Konjam neram aagum, poruma irukanum.)
echo.

set ICON_ARG=
if exist packaging\icon.ico set ICON_ARG=--icon packaging\icon.ico

jpackage --type app-image --name "Nixa Toolkit" --input dist ^
    --main-jar NixaToolkit.jar --main-class com.nixatoolkit.App ^
    --dest dist-app --app-version 1.0.12 ^
    --vendor "Nixa Toolkit" %ICON_ARG%
if errorlevel 1 (
    echo.
    echo [WARNING] jpackage app-image step fail aachu - aனா kவலைப்படாதேங்க,
    echo dist\NixaToolkit.jar already ready irukku, adha double-click pannalam
    echo ^(inda PC-la Java irundha போதும்^).
    echo.
    goto skip_installer
)
echo     OK - dist-app\Nixa Toolkit\Nixa Toolkit.exe ready ^(no Java venam^).
echo.

rem --- Step 4 (optional): a proper installer, needs WiX Toolset ---------------
echo [4/4] Trying to build a Windows installer ^(.exe^) too...
echo       ^(Idhukku WiX Toolset venum - illainna, idhu step skip aagum, adhுla
echo        problem illa - Step 3-la vandha app folder already use panniducha.^)
echo.
jpackage --type exe --name "Nixa Toolkit" --input dist ^
    --main-jar NixaToolkit.jar --main-class com.nixatoolkit.App ^
    --dest dist-installer --app-version 1.0.12 ^
    --vendor "Nixa Toolkit" --win-shortcut --win-menu %ICON_ARG% 2>nul
if not errorlevel 1 (
    echo     OK - dist-installer\ - oru setup .exe ready, adha share pannalam.
) else (
    echo     ^(WiX illama irundhadhala installer build aagala - adhu problem illa.^)
)
echo.

:skip_installer
echo ============================================
echo  Build mudinjuduchu!
echo ============================================
echo.
echo Use panna 3 vazhi irukku ^(edhavadhu 1 podhum^):
echo.
echo   1. dist-app\Nixa Toolkit\Nixa Toolkit.exe
echo        - Best option. Double-click pannunga - vera edhுவும் install panna
echo          venam, Java kூட venam.
echo.
echo   2. dist-installer\ ^(irundha^)
echo        - Setup .exe, install panni desktop shortcut vekkalam.
echo.
echo   3. dist\NixaToolkit.jar
echo        - "java -jar NixaToolkit.jar" nu run pannalam, aனா indha PC-la
echo          Java 17+ already irukanum.
echo.
pause
