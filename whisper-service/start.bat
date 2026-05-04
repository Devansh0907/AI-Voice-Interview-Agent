@echo off
echo Starting Whisper Transcription Service...
echo.

if not exist "venv" (
    echo Creating virtual environment...
    python -m venv venv
    call venv\Scripts\activate
    echo Installing dependencies...
    pip install -r requirements.txt
) else (
    call venv\Scripts\activate
)

echo.
echo Whisper service starting on http://localhost:8001
echo API docs at http://localhost:8001/docs
echo.
python main.py
