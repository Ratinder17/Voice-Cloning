package com.example.Voice.Cloning.controller;

import com.example.Voice.Cloning.service.Transcription;
import com.example.Voice.Cloning.service.Translation;
import com.example.Voice.Cloning.service.Segment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class VoiceController {

    @Autowired
    private Transcription transcriptionService;

    @Autowired
    private Translation translationService;

    @GetMapping("/translate/Video")
    public ResponseEntity<?> translateVideo(@RequestParam("filename") String filename,
                                            @RequestParam("language") String language) {
        try {
            // Step 1–2: Locate and extract audio
            File video = new File("src/main/resources/static/" + filename);
            String audioPath = transcriptionService.extractAudio(video);
            File audioFile = new File(audioPath);

            // Step 3: Transcribe to segments
            String segmentJson = transcriptionService.transcribe(audioFile);

            // Step 4: Parse segments
            ObjectMapper mapper = new ObjectMapper();
            Segment[] segments = mapper.readValue(segmentJson, Segment[].class);

            double lastEndTime = 0.0;

            // Step 5–7: Translate and synthesize each segment
            for (int i = 0; i < segments.length; i++) {
                Segment seg = segments[i];
                String translated = translationService.translate(seg.getText(), language);
                seg.setTranslatedText(translated); // Save translated version

                String segmentFile = "segment_" + i + ".wav";

                ProcessBuilder pb = new ProcessBuilder(
                        "python3", "voice_clone_segment.py",
                        translated,
                        String.valueOf(seg.getStart()),
                        String.valueOf(seg.getEnd()),
                        segmentFile,
                        language.toLowerCase(),
                        String.valueOf(lastEndTime)  // ➕ Added to sync silence
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[PYTHON] " + line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Failed to synthesize segment: " + i);
                }

                lastEndTime = seg.getEnd();  // ➕ Update for next segment
            }

            // Step 8: Merge all segments into one final audio
            Process mergeProcess = Runtime.getRuntime().exec("python3 merge_segments.py");
            mergeProcess.waitFor();

            // Step 9: Merge final audio with original video using ffmpeg
            String staticDir = "src/main/resources/static/";
            String finalAudio = staticDir + "final_cloned_synced.wav";
            String outputVideo = staticDir + "output_dubbed.mp4";

            ProcessBuilder ffmpegBuilder = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", "src/main/resources/static/" + filename,
                    "-i", finalAudio,
                    "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0",
                    "-shortest", outputVideo
            );
            ffmpegBuilder.redirectErrorStream(true);
            Process ffmpegProcess = ffmpegBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[FFMPEG] " + line);
                }
            }

            int ffmpegExit = ffmpegProcess.waitFor();
            if (ffmpegExit != 0) {
                throw new RuntimeException("FFmpeg failed to combine audio and video");
            }

            Files.copy(Paths.get(outputVideo),
                    Paths.get("target/classes/static/output_dubbed.mp4"),
                    StandardCopyOption.REPLACE_EXISTING);

            Files.copy(Paths.get(finalAudio),
                    Paths.get("target/classes/static/final_cloned_synced.wav"),
                    StandardCopyOption.REPLACE_EXISTING);

            // Step 10: Return path to the dubbed video and segment data
            return ResponseEntity.ok(Map.of(
                    "videoFile", "output_dubbed.mp4",
                    "segments", segments
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "An error occurred",
                    "details", e.getMessage()
            ));
        }
    }
}
/*
1. Load a video file
2. Extract the audio using extractAudio method in Transcription class
3. Transcribe audio into segments

5. Loop through each segment and translate each segment using Translation class.
6. Run Python TTS on each segment using voice_clone_segment.py
7. Save  audio as segment_i.wav where i tells us the number of segment

8. Run merge_segments.py to merge all the segments that were formed in the previous step

9. Combine merged audio with original video

10. Return response to frontend.
 */