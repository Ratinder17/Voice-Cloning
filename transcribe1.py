import sys
import whisper
import warnings
import json
import os

warnings.filterwarnings("ignore")

model = whisper.load_model("medium")
file_path = sys.argv[1]

sys.stdout = open(os.devnull, 'w')
sys.stderr = open(os.devnull, 'w')

result = model.transcribe(file_path, task="transcribe", verbose=False)

sys.stdout = sys.__stdout__

segments = result.get("segments", [])
output = []

for seg in segments:
    output.append({
        "start": seg["start"],
        "end": seg["end"],
        "text": seg["text"]
    })

print(json.dumps(output))
