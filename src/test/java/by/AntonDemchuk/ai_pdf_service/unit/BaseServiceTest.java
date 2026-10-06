package by.AntonDemchuk.ai_pdf_service.unit;

import by.AntonDemchuk.ai_pdf_service.entity.PDFFile;
import by.AntonDemchuk.ai_pdf_service.entity.ProcessingJob;
import by.AntonDemchuk.ai_pdf_service.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    protected final Long userId = 1L;
    protected final Long fileId = 10L;

    protected User mockedUser;
    protected PDFFile mockedPdfFile;
    protected ProcessingJob mockedProcessingJob;

    protected Pageable pageable;

    @BeforeEach
    void setUp() {

        pageable = PageRequest.of(0, 5);

        /* Mocks */
        mockedUser = mock(User.class);
        mockedPdfFile = mock(PDFFile.class);
        mockedProcessingJob = mock(ProcessingJob.class);
    }
}