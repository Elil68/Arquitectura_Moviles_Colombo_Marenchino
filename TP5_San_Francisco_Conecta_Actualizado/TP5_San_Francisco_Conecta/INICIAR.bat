@echo off
setlocal
title San Francisco Conecta - TP5
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
 echo No se encontro Node.js.
 pause
 exit /b 1
)
node -e "try{require('node:sqlite')}catch(e){console.error('Esta instalacion de Node no incluye SQLite integrado.');process.exit(1)}"
if errorlevel 1 (
 pause
 exit /b 1
)
node "%~dp0server.js" --open
if errorlevel 1 pause
