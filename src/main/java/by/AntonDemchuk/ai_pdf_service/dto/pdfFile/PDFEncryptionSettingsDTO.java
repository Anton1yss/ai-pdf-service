package by.AntonDemchuk.ai_pdf_service.dto.pdfFile;

import lombok.Data;

@Data
public class PDFEncryptionSettingsDTO {
    private String userPass;
    private String ownerPass;
    private boolean allowPrinting;
    private boolean allowModifyContents;
    private boolean allowCopy;
    private boolean allowModifyMetadata;
    private boolean allowModifyAnnotations;
    private boolean allowFillIn;
    private boolean allowScreenReaders;
    private boolean allowAssembly;
}
