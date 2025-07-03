from pydub import AudioSegment
import os
import re

# Match files like segment_0.wav, segment_1.wav...
segment_files = sorted([
    f for f in os.listdir() if re.match(r"segment_\d+\.wav", f)
], key=lambda x: int(re.findall(r"\d+", x)[0]))

# Merge them
final = AudioSegment.empty()

for file in segment_files:
    audio = AudioSegment.from_wav(file)
    final += audio

# Export final audio
final.export("src/main/resources/static/final_cloned_synced.wav", format="wav")
