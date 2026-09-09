package by.AntonDemchuk.ai_pdf_service.repository.auditLog;

import by.AntonDemchuk.ai_pdf_service.dto.auditLog.AuditLogSearchParams;
import by.AntonDemchuk.ai_pdf_service.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public class AuditLogSpecification {

    public static Specification<AuditLog> buildSpecification(AuditLogSearchParams params) {

        Specification<AuditLog> spec = Specification.where(null);

        if (params.getUserId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("user").get("id"), params.getUserId()));
        }

        if (params.getPdfFileId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("pdfFile").get("id"), params.getPdfFileId()));
        }

        if (params.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), params.getStatus()));
        }

        if (params.getAction() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("action"), params.getAction()));
        }

        if (params.getFromDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), params.getFromDate()));
        }

        if (params.getToDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), params.getToDate()));
        }

        spec = spec.and((root, query, cb) -> {
            query.orderBy(cb.desc(root.get("createdAt")));
            return null;
        });

        return spec;
    }
}