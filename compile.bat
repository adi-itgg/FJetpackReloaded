@echo off
set currentDir=%~dp0
set dirPlugin=G:\Server\Minecraft\1.19\plugins
call "%~dp0\gradlew.bat" shadowJar
set outputDir=%~dp0build\libs
cd /d "%outputDir%"
pushd "%outputDir%"
for /f "tokens=*" %%a in ('dir /b /od') do set newest=%%a
move /Y "%outputDir%\%newest%" "%dirPlugin%\%newest%"
echo Ouput Jar Location %outputDir%\%newest%
echo jar has Moved to %dirPlugin%\%newest%
popd
timeout 150 > nul
exit