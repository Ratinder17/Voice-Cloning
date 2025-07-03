import subprocess
import sys

video = sys.argv[1]
audio = sys.argv[2]
output = sys.argv[3]

subprocess.run([
    "ffmpeg", "-i", video, "-i", audio,
    "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0",
    "-shortest", output
])
