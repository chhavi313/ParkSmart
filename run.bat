@echo off
echo ==========================================================
echo Starting ParkSmart - Smart Parking Management System
echo ==========================================================
if exist target\parksmart-1.0.0.jar (
    java -jar target\parksmart-1.0.0.jar
) else (
    echo Building project first...
    call compile.bat
    java -jar target\parksmart-1.0.0.jar
)
pause
