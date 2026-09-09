package by.AntonDemchuk.ai_pdf_service.service;

import by.AntonDemchuk.ai_pdf_service.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    public String uploadDocument(MultipartFile file, User currentUser, String directory) throws IOException {

        String key = generateS3Key(currentUser.getId(), file.getOriginalFilename(), directory);

        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            return key;

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public String uploadDocument(byte[] documentBytes, String documentName, User currentUser, String directory) throws IOException {

        String key = generateS3Key(currentUser.getId(), documentName, directory);

        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    RequestBody.fromBytes(documentBytes));

            return key;

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public byte[] downloadDocument(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

        } catch (S3Exception e) {
            log.error("Failed to download document | key: {}", s3Key);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    public String generatePreSignedURL(String s3Key) {

        try {
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(r -> r.bucket(bucketName).key(s3Key))
                    .build();

            return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        }
    }

    public void deleteDocument(String s3Key) throws IOException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());

        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    private String generateS3Key(Long userId, String originalDocumentName, String directory) {
        String uuid = UUID.randomUUID().toString();
        String sanitizedDocumentName = originalDocumentName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%s/%d/%s-%s", directory, userId, uuid, sanitizedDocumentName);
    }
}