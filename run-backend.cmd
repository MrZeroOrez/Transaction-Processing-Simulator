@echo off
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run
