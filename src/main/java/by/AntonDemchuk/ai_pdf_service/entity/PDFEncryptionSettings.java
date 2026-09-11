package by.AntonDemchuk.ai_pdf_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PDFEncryptionSettings {

    @Builder.Default
    private boolean encrypted = false;

    @Builder.Default
    private boolean allowPrinting = true;

    @Builder.Default
    private boolean allowCopy = true;

    @Builder.Default
    private boolean allowModifyContents = true;

    @Builder.Default
    private boolean allowModifyAnnotations = true;

    @Builder.Default
    private boolean allowFillIn = true;

    @Builder.Default
    private boolean allowScreenReaders = true;

    @Builder.Default
    private boolean allowAssembly = true;
}