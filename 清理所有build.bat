@echo off 
for /F "delims=" %%i in ('dir /ad/b/s') do (
    if %%~ni==build (
        echo 正在删除……%%i
        rd /q/s %%i
    )
)
echo --------清理结束！--------
pause>nul