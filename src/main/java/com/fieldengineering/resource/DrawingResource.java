package com.fieldengineering.resource;

import com.fieldengineering.domain.DrawingEntity;
import com.fieldengineering.domain.PageMarkupEntity;
import com.fieldengineering.dto.DrawingResponseDto;
import com.fieldengineering.dto.PageMarkupDto;
import com.fieldengineering.service.StorageService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Path("/api/v1/drawings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DrawingResource {

    @Inject
    StorageService storageService;

    public static class FileUploadForm {
        @RestForm("file")
        public FileUpload file;

        @RestForm("name")
        @PartType(MediaType.TEXT_PLAIN)
        public String name;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadDrawing(FileUploadForm form) {
        if (form == null || form.file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"No file uploaded\"}")
                    .build();
        }

        File uploadedFile = form.file.uploadedFile().toFile();
        String originalFileName = form.file.fileName();
        String contentType = form.file.contentType();
        long fileSize = form.file.size();

        // 50MB max limit
        if (fileSize > 50 * 1024 * 1024) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"File exceeds 50MB limit\"}")
                    .build();
        }

        String lowerName = (originalFileName != null ? originalFileName : "").toLowerCase();
        String fileType;
        int pageCount = 1;

        if (lowerName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(contentType)) {
            fileType = "PDF";
            pageCount = countPdfPages(uploadedFile);
        } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || (contentType != null && contentType.startsWith("image/"))) {
            fileType = "IMAGE";
            pageCount = 1;
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Unsupported format. Please upload PDF, PNG, or JPG\"}")
                    .build();
        }

        String drawingId = "dwg-" + UUID.randomUUID().toString().substring(0, 8);
        String extension = lowerName.contains(".") ? lowerName.substring(lowerName.lastIndexOf(".")) : (fileType.equals("PDF") ? ".pdf" : ".png");
        String storageKey = drawingId + extension;

        try (InputStream is = new FileInputStream(uploadedFile)) {
            storageService.storeFile(storageKey, is, contentType, fileSize);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to persist file: " + e.getMessage() + "\"}")
                    .build();
        }

        String drawingName = form.name != null && !form.name.trim().isEmpty() 
                ? form.name.trim() 
                : (originalFileName != null ? originalFileName : "Untitled Drawing");

        DrawingEntity entity = new DrawingEntity();
        entity.id = drawingId;
        entity.name = drawingName;
        entity.fileKey = storageKey;
        entity.fileType = fileType;
        entity.pageCount = pageCount;
        entity.fileSize = fileSize;
        entity.ownerId = "engineer@field.internal";
        entity.createdAt = Instant.now();
        entity.persist();

        DrawingResponseDto dto = toDto(entity);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    public List<DrawingResponseDto> listDrawings() {
        return DrawingEntity.listAllOrdered().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getDrawing(@PathParam("id") String id) {
        DrawingEntity entity = DrawingEntity.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toDto(entity)).build();
    }

    @GET
    @Path("/{id}/file")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, "application/pdf", "image/png", "image/jpeg"})
    public Response getDrawingFile(@PathParam("id") String id) {
        DrawingEntity entity = DrawingEntity.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            InputStream is = storageService.getFile(entity.fileKey);
            String mime = "PDF".equals(entity.fileType) ? "application/pdf" : "image/png";
            return Response.ok(is, mime)
                    .header("Content-Disposition", "inline; filename=\"" + entity.name + "\"")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"File not found on storage\"}").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteDrawing(@PathParam("id") String id) {
        DrawingEntity entity = DrawingEntity.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            storageService.deleteFile(entity.fileKey);
        } catch (Exception ignored) {}

        PageMarkupEntity.delete("drawingId", id);
        entity.delete();

        return Response.noContent().build();
    }

    // --- Page Markup Endpoints ---

    @GET
    @Path("/{id}/pages/{pageNumber}/markup")
    public Response getPageMarkup(@PathParam("id") String drawingId, @PathParam("pageNumber") int pageNumber) {
        DrawingEntity drawing = DrawingEntity.findById(drawingId);
        if (drawing == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Drawing not found\"}").build();
        }

        PageMarkupEntity markup = PageMarkupEntity.findByDrawingAndPage(drawingId, pageNumber);
        if (markup == null) {
            PageMarkupDto emptyDto = new PageMarkupDto();
            emptyDto.drawingId = drawingId;
            emptyDto.pageNumber = pageNumber;
            emptyDto.payload = "{\"strokes\":[],\"labels\":[]}";
            emptyDto.version = 1;
            emptyDto.updatedAt = Instant.now();
            return Response.ok(emptyDto).build();
        }

        PageMarkupDto dto = new PageMarkupDto();
        dto.id = markup.id;
        dto.drawingId = markup.drawingId;
        dto.pageNumber = markup.pageNumber;
        dto.payload = markup.payload;
        dto.version = markup.version;
        dto.updatedAt = markup.updatedAt;

        return Response.ok(dto).build();
    }

    @PUT
    @Path("/{id}/pages/{pageNumber}/markup")
    @Transactional
    public Response savePageMarkup(
            @PathParam("id") String drawingId,
            @PathParam("pageNumber") int pageNumber,
            PageMarkupDto requestDto) {

        DrawingEntity drawing = DrawingEntity.findById(drawingId);
        if (drawing == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Drawing not found\"}").build();
        }

        PageMarkupEntity existing = PageMarkupEntity.findByDrawingAndPage(drawingId, pageNumber);

        if (existing == null) {
            existing = new PageMarkupEntity();
            existing.id = "pm-" + UUID.randomUUID().toString();
            existing.drawingId = drawingId;
            existing.pageNumber = pageNumber;
            existing.payload = requestDto.payload != null ? requestDto.payload : "{\"strokes\":[],\"labels\":[]}";
            existing.version = 1;
            existing.updatedAt = Instant.now();
            existing.persist();

            requestDto.id = existing.id;
            requestDto.version = existing.version;
            requestDto.updatedAt = existing.updatedAt;
            return Response.ok(requestDto).build();
        }

        // Optimistic Locking Check
        if (requestDto.version > 0 && requestDto.version != existing.version) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"Optimistic lock conflict\", \"serverVersion\": " + existing.version + "}")
                    .build();
        }

        existing.payload = requestDto.payload;
        existing.version += 1;
        existing.updatedAt = Instant.now();

        requestDto.id = existing.id;
        requestDto.version = existing.version;
        requestDto.updatedAt = existing.updatedAt;

        return Response.ok(requestDto).build();
    }

    private int countPdfPages(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 2 * 1024 * 1024)];
            raf.readFully(buffer);
            String text = new String(buffer, StandardCharsets.ISO_8859_1);

            Pattern p = Pattern.compile("/Type\\s*/Pages.*?/Count\\s+(\\d+)", Pattern.DOTALL);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return Math.max(1, Integer.parseInt(m.group(1)));
            }

            Pattern p2 = Pattern.compile("/Type\\s*/Page(?!s)");
            Matcher m2 = p2.matcher(text);
            int count = 0;
            while (m2.find()) {
                count++;
            }
            return Math.max(1, count);
        } catch (Exception e) {
            return 1;
        }
    }

    private DrawingResponseDto toDto(DrawingEntity entity) {
        DrawingResponseDto dto = new DrawingResponseDto();
        dto.id = entity.id;
        dto.name = entity.name;
        dto.fileType = entity.fileType;
        dto.pageCount = entity.pageCount;
        dto.fileSize = entity.fileSize;
        dto.ownerId = entity.ownerId;
        dto.createdAt = entity.createdAt;
        dto.fileUrl = "/api/v1/drawings/" + entity.id + "/file";
        return dto;
    }
}
