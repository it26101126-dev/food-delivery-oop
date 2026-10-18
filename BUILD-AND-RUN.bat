@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo A JDK is needed to rebuild Java code. Check javac -version.
  pause
  exit /b 1
)
echo Stop the previous FoodDelivery window before rebuilding.
if not exist build\classes mkdir build\classes
if not exist app mkdir app
javac --release 17 -encoding UTF-8 -cp "lib\gson-2.13.2.jar" -d build\classes @sources.txt
if errorlevel 1 goto failed
jar --create --file app\fooddelivery.jar --manifest manifest.mf -C build\classes .
if errorlevel 1 goto failed
call RUN-ME.bat
exit /b 0
:failed
echo Build failed. Read the error above. The old app was not started.
pause
exit /b 1
