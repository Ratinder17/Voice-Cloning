import sys
import os
from pydub import AudioSegment
from pydub.effects import speedup
import torch
from torch.serialization import add_safe_globals
from transformers import T5Tokenizer, T5ForConditionalGeneration

from TTS.tts.configs.xtts_config import XttsConfig, XttsAudioConfig
from TTS.config.shared_configs import BaseDatasetConfig
from TTS.tts.models.xtts import XttsArgs

add_safe_globals([XttsConfig, XttsAudioConfig, BaseDatasetConfig, XttsArgs])

from TTS.api import TTS

# Inputs
translated_text = sys.argv[1]
start_time = float(sys.argv[2])
end_time = float(sys.argv[3])
output_path = sys.argv[4]
language = sys.argv[5]
last_end_time = float(sys.argv[6]) if len(sys.argv) > 6 else 0.0

speaker_wav = "src/main/resources/static/sample_audio.wav"
temp_file = "temp_segment.wav"

# Load summarizer
tokenizer = T5Tokenizer.from_pretrained("t5-small")
model = T5ForConditionalGeneration.from_pretrained("t5-small")

def summarize(text):
    input_text = "summarize: " + text.strip()
    inputs = tokenizer.encode(input_text, return_tensors="pt", max_length=512, truncation=True)
    summary_ids = model.generate(inputs, max_length=60, min_length=15, length_penalty=2.0, num_beams=4, early_stopping=True)
    return tokenizer.decode(summary_ids[0], skip_special_tokens=True)

# Load TTS
try:
    tts = TTS(model_name="tts_models/multilingual/multi-dataset/xtts_v2")
except Exception as e:
    print(f"[PYTHON] Error loading TTS model: {e}")
    sys.exit(1)

# Target duration in ms
target_duration_ms = int((end_time - start_time) * 1000)

def generate_speech(text):
    try:
        tts.tts_to_file(text=text, speaker_wav=speaker_wav, language=language, file_path=temp_file)
        return AudioSegment.from_wav(temp_file)
    except Exception as e:
        print(f"[PYTHON] Error generating TTS: {e}")
        sys.exit(1)

speech = generate_speech(translated_text)

if len(speech) > target_duration_ms + 100:
    summarized_text = summarize(translated_text)
    print(f"[PYTHON] Summarized text: {summarized_text}")
    speech = generate_speech(summarized_text)

actual_duration = len(speech)
if actual_duration > target_duration_ms + 100:
    speed_factor = actual_duration / target_duration_ms
    if speed_factor > 2.0:
        print("[PYTHON] Speech too long — trimming")
        speech = speech[:target_duration_ms]
    else:
        print(f"[PYTHON] Speeding up: factor={speed_factor:.2f}")
        speech = speedup(speech, playback_speed=speed_factor)
elif actual_duration < target_duration_ms - 100:
    print("[PYTHON] Padding end silence to match duration")
    pad_ms = target_duration_ms - actual_duration
    speech += AudioSegment.silent(duration=pad_ms)

gap_duration = max(0, start_time - last_end_time)
gap_padding_ms = int(gap_duration * 1000)
if gap_padding_ms > 0:
    speech = AudioSegment.silent(duration=gap_padding_ms) + speech


if len(sys.argv) > 7:
    video_path = sys.argv[7]

    def get_video_duration(path):
        import subprocess
        try:
            result = subprocess.run(
                ["ffprobe", "-v", "error", "-show_entries", "format=duration",
                 "-of", "default=noprint_wrappers=1:nokey=1", path],
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT
            )
            return float(result.stdout.decode().strip())
        except Exception as e:
            print(f"[PYTHON] Failed to get video duration: {e}")
            return None

    video_duration = get_video_duration(video_path)
    if video_duration:
        final_duration_ms = int(video_duration * 1000)
        speech = speech[:final_duration_ms]  # Trim if longer
        if len(speech) < final_duration_ms:
            padding = AudioSegment.silent(duration=final_duration_ms - len(speech))
            speech += padding
        print(f"[PYTHON] Trimmed/padded final audio to {video_duration}s")

    speech.export(output_path, format="wav")

speech.export(output_path, format="wav")
os.remove(temp_file)
