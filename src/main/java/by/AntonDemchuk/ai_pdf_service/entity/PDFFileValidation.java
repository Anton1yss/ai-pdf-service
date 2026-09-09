package by.AntonDemchuk.ai_pdf_service.entity;

import lombok.Getter;

import java.util.Set;

@Getter
public class PDFFileValidation {

     public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf"
    );

}
