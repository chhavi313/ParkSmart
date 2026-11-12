@echo off
echo ==========================================================
echo Compiling ParkSmart (Java 17)
echo ==========================================================

if not exist target\classes mkdir target\classes

javac -cp "gson-2.10.1.jar" -d target/classes src/main/java/com/parksmart/*.java src/main/java/com/parksmart/model/*.java src/main/java/com/parksmart/service/*.java src/main/java/com/parksmart/storage/*.java src/main/java/com/parksmart/server/*.java src/main/java/com/parksmart/util/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [OK] Java source files compiled successfully.

if not exist target\classes\static mkdir target\classes\static
xcopy /E /I /Y src\main\resources\static target\classes\static >nul

echo [OK] Static web resources copied.

cd target\classes
jar -xf ..\..\gson-2.10.1.jar
cd ..\..
jar --create --file target/parksmart-1.0.0.jar --main-class com.parksmart.SmartParkingApplication -C target/classes .

echo [OK] Packaged standalone executable target/parksmart-1.0.0.jar
echo ==========================================================
echo Build completed successfully!
echo You can run the application with: java -jar target/parksmart-1.0.0.jar
echo Or simply run: run.bat
echo ==========================================================
