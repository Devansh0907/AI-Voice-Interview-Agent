from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel
import shutil
import os
import uuid
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Whisper Transcription Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load model on startup — "base" is ~150MB, good balance of speed/accuracy
# Options: "tiny", "base", "small", "medium", "large-v3"
logger.info("Loading Whisper model (base)... This may take a moment on first run.")
model = WhisperModel("base", device="cpu", compute_type="int8")
logger.info("Whisper model loaded successfully!")


@app.get("/health")
async def health():
    return {"status": "ok", "model": "base"}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")

    # Save uploaded file to temp location
    temp_filename = f"temp_{uuid.uuid4().hex}_{file.filename}"
    try:
        with open(temp_filename, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        logger.info(f"Transcribing file: {file.filename}")
        segments, info = model.transcribe(temp_filename, beam_size=5, vad_filter=True)

        transcript = " ".join([segment.text for segment in segments]).strip()
        logger.info(f"Transcription complete. Language: {info.language}, Length: {len(transcript)} chars")

        return {
            "text": transcript,
            "language": info.language,
            "language_probability": round(info.language_probability, 2)
        }
    except Exception as e:
        logger.error(f"Transcription failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}")
    finally:
        if os.path.exists(temp_filename):
            os.remove(temp_filename)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
