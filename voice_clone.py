from TTS.api import TTS

# Read translated text
with open("translated_output.txt", "r") as f:
    translated_text = f.read().strip()

tts = TTS(model_name="tts_models/multilingual/multi-dataset/your_tts")

tts.tts_to_file(
    text=translated_text,
    speaker_wav="src/main/resources/static/sample_audio.wav",
    file_path="src/main/resources/static/cloned_output.wav",
    language="en"
)
