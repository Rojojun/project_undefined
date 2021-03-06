package com.finalproject.breeding.video.util;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VideoEncode {
    FFmpeg ffmpeg;
    FFprobe ffprobe;
    @PostConstruct
    public void init() {
        try {
            // 1. 썸네일을 추출하고자 하는 영상 파일의 절대 경로 설정
            //String inputPath = "Users/hojunna/Download/my/";
            //String outputPath = "/usr/local/video/thumbnail/";

            // 2. FFmpeg 파일 경로 설정

            String ffmpegBasePath = "/opt/homebrew/bin/";

            /*
            // MAC 로컬 경로
            ffmpeg = new FFmpeg("/opt/homebrew/bin/ffmpeg");		// ffmpeg.exe 파일 경로
            ffprobe = new FFprobe("/opt/homebrew/bin/ffprobe");	// ffprobe.exe 파일 경로
            */

            ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
            ffprobe = new FFprobe("/usr/bin/ffprobe");

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void videoEncode(String fileUrl, String savePath, String startTime) throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe(fileUrl);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(/*"/Users/hojunna/Desktop/" + */fileUrl)     					// 썸네일 이미지 추출에 사용할 영상 파일의 절대 경로
                .addExtraArgs("-ss", startTime) 			// 영상에서 추출하고자 하는 시간 - 00:00:01은 1초를 의미

                .overrideOutputFiles(true)					// output 파일을 덮어쓸 것인지 여부(false일 경우, output path에 해당 파일이 존재할 경우 예외 발생 - File 'C:/Users/Desktop/test.png' already exists. Exiting.)

                .addOutput(/*"/Users/hojunna/Desktop/" + */savePath) 		// 저장 절대 경로(확장자 미 지정 시 예외 발생 - [NULL @ 000002cc1f9fa500] Unable to find a suitable output format for 'C:/Users/Desktop/test')
                .setFormat("mp4")
/*                .addExtraArgs("-tag:v", "hvc1")
                .setVideoCodec("libx265")*/

                //.setVideoCodec("h264_vda")
                .setVideoFilter("select='gte(n\\,5)',scale=300:-2")
                .setDuration(15, TimeUnit.SECONDS)
                .setVideoFrameRate(FFmpeg.FPS_24)


                //.setVideoFilter("select='gte(n\\,10)',scale=200:-1")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg,ffprobe);
        executor.createJob(builder).run();
    }

    public HashMap<String, Object> exportThumbnail(String fileUrl, String savePath, String extractTime) throws Exception {

        // 3. FFmpegBuilder를 통해 FFmpeg 명령어를 만들 수 있음
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(/*"/Users/hojunna/Desktop/" +*/ fileUrl)     					// 썸네일 이미지 추출에 사용할 영상 파일의 절대 경로
                //.overrideOutputFiles(true)					// output 파일을 덮어쓸 것인지 여부(false일 경우, output path에 해당 파일이 존재할 경우 예외 발생 - File 'C:/Users/Desktop/test.png' already exists. Exiting.)
                .addExtraArgs("-ss", extractTime) 			// 영상에서 추출하고자 하는 시간 - 00:00:01은 1초를 의미
                .addOutput(/*"/Users/hojunna/Desktop/" +*/ savePath) 		// 저장 절대 경로(확장자 미 지정 시 예외 발생 - [NULL @ 000002cc1f9fa500] Unable to find a suitable output format for 'C:/Users/Desktop/test')
                //.setVideoResolution(720,720)  // height를 기준으로 Resolution 설정
                .setFrames(1)
                .setVideoFilter("select='gte(n\\,5)',scale=300:-1")
                //.setVideoFilter("select='gte(n\\,10)',scale=200:-1")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);		// FFmpeg 명령어 실행을 위한 FFmpegExecutor 객체 생성
        executor.createJob(builder).run();									// one-pass encode
        //executor.createTwoPassJob(builder).run();							// two-pass encode*/

        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        return resultMap;
    }

    public int getVideoLength(String fileUrl) throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe(fileUrl);

        return (int) probeResult.streams.get(0).duration;
    }

    public int getVideoWidth(String fileUrl) throws IOException {
        FFmpegProbeResult probeResult = ffprobe.probe(fileUrl);

        return (int) probeResult.streams.get(0).width;
    }
}
