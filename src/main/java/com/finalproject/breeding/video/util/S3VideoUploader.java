package com.finalproject.breeding.video.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.finalproject.breeding.board.model.Reels;
import com.finalproject.breeding.board.repository.ReelsRepository;
import com.finalproject.breeding.error.CustomException;
import com.finalproject.breeding.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3VideoUploader {

    private final AmazonS3 amazonS3;
    private final AmazonS3Client amazonS3Client;
    private final VideoEncode videoEncode;
    private final ReelsRepository reelsRepository;
    @Value("${cloud.aws.s3.bucket}")
    public String bucket;  // S3 버킷 이름

    public String uploadThumbnail(MultipartFile multipartFile, String dirName, Boolean isVideo, String thumnailTime) throws Exception {
        File uploadFile = convert(multipartFile).orElseThrow(() -> new IllegalArgumentException("썸네일 추출 실패"));

        if (isVideo) {
            File convertFile = new File(System.getProperty("user.dir") + "/" + multipartFile.getOriginalFilename());
            String randomVideoName = UUID.randomUUID().toString();

            if (convertFile.createNewFile()) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(convertFile)) {
                    // File의 데이터를 사용하기 위한 OutputStream 선언
                    // FileOutputStream : 파일로 바이트 단위의 출력을 내보내는 클래스
                    fileOutputStream.write(multipartFile.getBytes());
                }
            }
            videoEncode.exportThumbnail(convertFile.getAbsolutePath(), System.getProperty("user.dir") + "/thumbnail" + multipartFile.getName() + ".png", thumnailTime);
            File thumbnailFile = new File(System.getProperty("user.dir") + "/thumbnail" + multipartFile.getName() + ".png");

            removeNewFile(uploadFile);
            return upload(thumbnailFile, dirName, false, randomVideoName);
        } else {
            return upload(uploadFile, dirName, false, UUID.randomUUID().toString());
        }
    }

    public String upload(MultipartFile multipartFile, String dirName, Boolean isVideo, String getThumbnailTime, String getStartPoint) throws Exception {
        File uploadFile = convert(multipartFile).orElseThrow(() -> new IllegalArgumentException("비디오 컨버트 실패"));
        String secValidation = "업로드 하려는 비디오의 길이가 4초보다 짧거나 300초를 초과합니다.";

        if (isVideo) {
            File convertFile = new File(System.getProperty("user.dir") + "/" + multipartFile.getOriginalFilename());
            String randomVideoName = UUID.randomUUID().toString();
            //int videoLength = videoEncode.getVideoLength(convertFile.getAbsolutePath());
            //int videoWidth = videoEncode.getVideoWidth(convertFile.getAbsolutePath());

            if (convertFile.createNewFile()) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(convertFile)) {
                    // File의 데이터를 사용하기 위한 OutputStream 선언
                    // FileOutputStream : 파일로 바이트 단위의 출력을 내보내는 클래스
                    fileOutputStream.write(multipartFile.getBytes());
                }
            }

            // 업로드 된 비디오 길이 Validation Check
            if (videoEncode.getVideoLength(convertFile.getAbsolutePath()) < 4 || videoEncode.getVideoLength(convertFile.getAbsolutePath()) > 300) {
                log.error(secValidation);
                throw new CustomException(ErrorCode.TIME_VALIDATION_WRONG);
            } else if (videoEncode.getVideoWidth(convertFile.getAbsolutePath()) < 300) {
                log.error("업로드 하려는 비디오의 화질이 300pixel 미만입니다.");
                throw new CustomException(ErrorCode.RESOLUTION_VALIDAION_WRONG);
            }

            videoEncode.videoEncode(convertFile.getAbsolutePath(), System.getProperty("user.dir") + "/video" + multipartFile.getOriginalFilename(), getStartPoint);
            // videoEncode.exportThumbnail(convertFile.getAbsolutePath(), System.getProperty("user.dir") + "/thumbnail" + multipartFile.getName() + ".png", getThumbnailTime);
            File file = new File(System.getProperty("user.dir") + "/video" + multipartFile.getOriginalFilename());
            // File thumbnailFile = new File(System.getProperty("user.dir") + "/thumbnail" + multipartFile.getName() + ".png");

            removeNewFile(uploadFile);
            if (videoEncode.getVideoLength(file.getAbsolutePath()) < 15) {
                return upload(file, dirName, true, randomVideoName);
            }
/*            File shortFile = new File(System.getProperty("user.dir") + "/video" + multipartFile.getOriginalFilename());
            upload(shortFile,dirName,true,randomVideoName);*/

            return upload(file, dirName, false, randomVideoName);

        } else {
            return upload(uploadFile, dirName, false, UUID.randomUUID().toString());
        }
    }

    // S3로 비디오 and 썸네일 파일 업로드하기
    // 15초 미만일경우 isShort 비디오 및 썸네일 업로드
    public String upload(File uploadFile, String dirName, Boolean isShort, String uuid) {
        if (isShort) {
            String fileName = dirName + "/" + uuid + "short" + "." + uploadFile.getName().substring(uploadFile.getName().lastIndexOf(".") + 1);   // S3에 저장된 파일 이름
            return putS3(uploadFile, fileName);
        }
    /* String fileName = dirName + "/" + uuid +".mp4";
       String fileName = dirName + "/" + uuid + ".png"; */
        String fileName = dirName + "/" + uuid + "." + uploadFile.getName().substring(uploadFile.getName().lastIndexOf(".") + 1);   // S3에 저장된 파일 이름
        return putS3(uploadFile, fileName);
    }
    // S3로 업로드
    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(CannedAccessControlList.PublicRead));
        removeNewFile(uploadFile);
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    // S3로 삭제
    public void deleteS3(String fileName) {
        Boolean isExistObject = amazonS3Client.doesObjectExist(bucket, fileName);
        if (isExistObject) {
            amazonS3Client.deleteObject(bucket, fileName);
        } else {
            log.warn("삭제할 s3파일이 존재하지 않습니다.");
        }
    }

    public S3Object getObject(String fileName) {
        Boolean isExistObject = amazonS3Client.doesObjectExist(bucket, fileName);
        if (isExistObject) {
            return amazonS3Client.getObject(bucket, fileName);
        } else {
            throw new IllegalArgumentException("s3 버킷에 파일이 존재하지 않습니다.");
        }
    }

    // 로컬에 저장된 이미지 지우기
    public void removeNewFile(File targetFile) {
        targetFile.delete();
        log.info("{} delete success", targetFile.getName());
    }

    public Optional<File> convert(MultipartFile multipartFile) throws IOException {
        System.out.println(System.getProperty("user.dir") + "/");
        System.out.println(multipartFile.getOriginalFilename());
        File convertFile = new File(System.getProperty("user.dir") + "/" + multipartFile.getOriginalFilename());

        System.out.println(System.getProperty("user.dir") + "/" + multipartFile.getOriginalFilename());
        // 바로 위에서 지정한 경로에 conver된 video가 생성됨, 경로의 문제가 있으면 생성 불가능
        /*if (convertFile.createNewFile()) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(convertFile)) {
                // File의 데이터를 사용하기 위한 OutputStream 선언
                // FileOutputStream : 파일로 바이트 단위의 출력을 내보내는 클래스
                fileOutputStream.write(multipartFile.getBytes());
            }
           */
        return Optional.of(convertFile);
    }

    public void remove(Long id) throws UnsupportedEncodingException {
        Reels reels = reelsRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("동영상 및 썸네일을 찾을 수 없습니다."));
        String filename = URLDecoder.decode(reels.getVideo().split("/reels")[1], "UTF-8");
        String thumbname = URLDecoder.decode(reels.getTitleImg().split("/thumbnail")[1], "UTF-8");


        if (amazonS3.doesObjectExist(bucket, reels.getVideo())) {
            throw new AmazonS3Exception("Object " + reels.getVideo() + " does not exist!");
        }

        amazonS3.deleteObject(bucket, filename);
        amazonS3.deleteObject(bucket, thumbname);

        reelsRepository.delete(reels);
    }
}
/*        return Optional.empty();

    }*/

