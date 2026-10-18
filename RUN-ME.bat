@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install Java 17 or later and reopen this folder.
  pause
  exit /b 1
)
echo FoodDelivery Simple - http://localhost:8081
echo Wait for FoodDelivery ready, then open the address above.
echo Keep this window open while using the website.
java -jar "app\fooddelivery.jar"
pause
