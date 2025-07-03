package com.example.Voice.Cloning.service;

import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class Transcription {
    public String transcribe(File file) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("python3", "transcribe1.py", file.getAbsolutePath())
                .redirectErrorStream(true)
                .start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Transcription failed with exit code: " + exitCode);
        }

        return output.toString().trim();
    }

    public String extractAudio(File videoFile) throws IOException, InterruptedException {
        String audioFilename = videoFile.getName().replaceAll("\\.mp4$", "") + "_audio.wav";
        File audioFile = new File(videoFile.getParent(), audioFilename);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoFile.getAbsolutePath(),
                "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                audioFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (reader.readLine() != null) {
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Exit code " + exitCode);
        }

        return audioFile.getAbsolutePath();
    }

}

/*
THOUGHT PROCESS
A) Extract Audio from the video file -
1. CREATE A NEW AUDIO FILE (for now keeping the same name as .mp4 source file)

2. Use PROCESSBUILDER to automate the python command

3.Start the process and read the output stream using a BufferedReader

4. If the process ends with a code other than 0, return appropriate message.

5. otherwise return the absolute path of new audio file. (Absolute path is important)

B) transcription - Use extracted audio for transcription
1. AGAIN NEED TO AUTOMATE THE PYTHON COMMAND - CAN BE DONE USING A PROCESSBUILDER

2. READ THE DATE. 2 Methods:
   a) BufferedReader - Helpful in processing output line by line in cases where transcription is very long.

   b)InputStream.readAllBytes() - Reads stream into a bite array but cannot be used for large outputs.

   Decision Process -
   InputStream may seem like a good choice as of now since the sample recordings are very short.
   However, BufferedReader offers more flexibility and will not cause any problems if longer videos are to be used in future.

   Verdict - Use BufferedReader instead of InputStream

3. If the process finishes with exit code other than 0, meaning that process didnt complete, throw exception with appropriate message

4. Return output of transcription operation as a string

 */