package by.AntonDemchuk.ai_pdf_service.unit.service;

import by.AntonDemchuk.ai_pdf_service.service.S3Service;
import by.AntonDemchuk.ai_pdf_service.unit.BaseServiceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest extends BaseServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String S3_KEY = "originals/1/some-key.pdf";

    @InjectMocks
    private S3Service s3Service;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @BeforeEach
    void setUpBucket() {
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET);
    }

    @Test
    public void uploadDocument_shouldUploadMultipartFile_andReturnKey() throws IOException {

        /* Arranging */
        MockMultipartFile file = new MockMultipartFile("file", "my report.pdf", "application/pdf", "content".getBytes());

        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        String key = s3Service.uploadDocument(file, mockedUser, "originals");

        /* Asserting & Verifying */
        assertTrue(key.matches("originals/1/[0-9a-f-]{36}-my_report\\.pdf"), "Unexpected key: " + key);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(key, captor.getValue().key());
    }

    @Test
    public void uploadDocument_shouldSanitizeFileName() throws IOException {

        /* Arranging */
        MockMultipartFile file = new MockMultipartFile("file", "../we!rd name (1).pdf", "application/pdf", "content".getBytes());

        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        String key = s3Service.uploadDocument(file, mockedUser, "originals");

        /* Asserting */
        assertFalse(key.contains(" "));
        assertFalse(key.contains("("));
        assertFalse(key.contains("!"));
        assertTrue(key.endsWith("-.._we_rd_name__1_.pdf"), "Unexpected key: " + key);
    }

    @Test
    public void uploadDocument_shouldThrowRuntimeException_whenS3Fails_forMultipartFile() throws IOException {

        /* Arranging */
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes());

        when(mockedUser.getId()).thenReturn(userId);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("s3 down").build());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> s3Service.uploadDocument(file, mockedUser, "originals"));
        assertEquals("Failed to upload file to S3", ex.getMessage());
    }

    @Test
    public void uploadDocument_shouldPropagateIOException_whenFileCannotBeRead() throws IOException {

        /* Arranging */
        MultipartFile file = mock(MultipartFile.class);

        when(mockedUser.getId()).thenReturn(userId);
        when(file.getOriginalFilename()).thenReturn("report.pdf");
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        /* Acting & Asserting */
        assertThrows(IOException.class, () -> s3Service.uploadDocument(file, mockedUser, "originals"));

        /* Verifying */
        verifyNoInteractions(s3Client);
    }

    @Test
    public void uploadDocument_shouldUploadBytes_andReturnKey() throws IOException {

        /* Arranging */
        byte[] bytes = "pdf-bytes".getBytes();

        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        String key = s3Service.uploadDocument(bytes, "report.pdf", mockedUser, "processed");

        /* Asserting & Verifying */
        assertTrue(key.matches("processed/1/[0-9a-f-]{36}-report\\.pdf"), "Unexpected key: " + key);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
        assertEquals(BUCKET, requestCaptor.getValue().bucket());
        assertEquals(key, requestCaptor.getValue().key());
        assertEquals(bytes.length, bodyCaptor.getValue().optionalContentLength().orElse(-1L));
    }

    @Test
    public void uploadDocument_shouldGenerateUniqueKeys_forSameFileName() throws IOException {

        /* Arranging */
        when(mockedUser.getId()).thenReturn(userId);

        /* Acting */
        String first = s3Service.uploadDocument(new byte[]{1}, "report.pdf", mockedUser, "processed");
        String second = s3Service.uploadDocument(new byte[]{1}, "report.pdf", mockedUser, "processed");

        /* Asserting */
        assertNotEquals(first, second);
    }

    @Test
    public void uploadDocument_shouldThrowRuntimeException_whenS3Fails_forBytes() {

        /* Arranging */
        when(mockedUser.getId()).thenReturn(userId);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("s3 down").build());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> s3Service.uploadDocument(new byte[]{1}, "report.pdf", mockedUser, "processed"));
        assertEquals("Failed to upload file to S3", ex.getMessage());
    }

    @Test
    public void downloadDocument_shouldReturnBytes() {

        /* Arranging */
        byte[] bytes = "pdf-bytes".getBytes();
        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(response);

        /* Acting */
        byte[] res = s3Service.downloadDocument(S3_KEY);

        /* Asserting & Verifying */
        assertArrayEquals(bytes, res);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(S3_KEY, captor.getValue().key());
    }

    @Test
    public void downloadDocument_shouldThrowRuntimeException_whenS3Fails() {

        /* Arranging */
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("not found").build());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class, () -> s3Service.downloadDocument(S3_KEY));
        assertEquals("Failed to download file from S3", ex.getMessage());
    }

    @Test
    public void generatePreSignedURL_shouldReturnUrl() throws Exception {

        /* Arranging */
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);

        when(presigned.url()).thenReturn(URI.create("https://s3.example.com/test-bucket/key?signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        /* Acting */
        String res = s3Service.generatePreSignedURL(S3_KEY);

        /* Asserting & Verifying */
        assertEquals("https://s3.example.com/test-bucket/key?signature=abc", res);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertEquals(Duration.ofMinutes(60), captor.getValue().signatureDuration());
        assertEquals(BUCKET, captor.getValue().getObjectRequest().bucket());
        assertEquals(S3_KEY, captor.getValue().getObjectRequest().key());
    }

    @Test
    public void generatePreSignedURL_shouldThrowRuntimeException_whenS3Fails() {

        /* Arranging */
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("denied").build());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class, () -> s3Service.generatePreSignedURL(S3_KEY));
        assertEquals("Failed to generate pre-signed URL", ex.getMessage());
    }

    @Test
    public void deleteDocument_shouldDeleteObject() throws IOException {

        /* Acting */
        s3Service.deleteDocument(S3_KEY);

        /* Verifying */
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(S3_KEY, captor.getValue().key());
    }

    @Test
    public void deleteDocument_shouldThrowRuntimeException_whenS3Fails() {

        /* Arranging */
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("denied").build());

        /* Acting & Asserting */
        RuntimeException ex = assertThrows(RuntimeException.class, () -> s3Service.deleteDocument(S3_KEY));
        assertEquals("Failed to delete file from S3", ex.getMessage());
    }
}