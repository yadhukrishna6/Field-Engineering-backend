package com.fieldengineering.resource;

import com.fieldengineering.domain.AnnotationCommentEntity;
import com.fieldengineering.domain.MarkupEntity;
import com.fieldengineering.domain.MarkupLayerEntity;
import com.fieldengineering.dto.AnnotationBatchRequest;
import com.fieldengineering.dto.AnnotationBatchResponse;
import com.fieldengineering.dto.AnnotationCommentDto;
import com.fieldengineering.dto.AnnotationStatusUpdateDto;
import io.quarkus.panache.common.Parameters;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/markups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarkupResource {

    @GET
    public List<MarkupEntity> listMarkups(
            @QueryParam("drawingId") String drawingId,
            @QueryParam("revisionId") String revisionId,
            @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("layer") String layer,
            @QueryParam("status") String status,
            @QueryParam("since") String sinceStr) {

        StringBuilder query = new StringBuilder("deleted = false");
        Parameters params = new Parameters();

        if (drawingId != null && !drawingId.isEmpty()) {
            query.append(" and drawingId = :drawingId");
            params.and("drawingId", drawingId);
        }
        if (revisionId != null && !revisionId.isEmpty()) {
            query.append(" and revisionId = :revisionId");
            params.and("revisionId", revisionId);
        }
        if (pageNumber != null) {
            query.append(" and pageNumber = :pageNumber");
            params.and("pageNumber", pageNumber);
        }
        if (layer != null && !layer.isEmpty()) {
            query.append(" and layer = :layer");
            params.and("layer", layer);
        }
        if (status != null && !status.isEmpty()) {
            query.append(" and status = :status");
            params.and("status", status);
        }
        if (sinceStr != null && !sinceStr.isEmpty()) {
            try {
                Instant since = Instant.parse(sinceStr);
                query.append(" and updatedAt >= :since");
                params.and("since", since);
            } catch (Exception ignored) {}
        }

        return MarkupEntity.list(query.toString(), params);
    }

    @GET
    @Path("/{id}")
    public Response getMarkup(@PathParam("id") String id) {
        MarkupEntity markup = MarkupEntity.findById(id);
        if (markup == null || markup.deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(markup).build();
    }

    @POST
    @Transactional
    public Response createMarkup(MarkupEntity markup) {
        if (markup.id == null || markup.id.isEmpty()) {
            markup.id = UUID.randomUUID().toString();
        }
        markup.createdAt = Instant.now();
        markup.updatedAt = Instant.now();
        markup.version = 1;
        markup.deleted = false;
        if (markup.status == null || markup.status.isEmpty()) {
            markup.status = "Open";
        }
        markup.persist();
        return Response.status(Response.Status.CREATED).entity(markup).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateMarkup(@PathParam("id") String id, MarkupEntity updated) {
        MarkupEntity existing = MarkupEntity.findById(id);
        if (existing == null || existing.deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Optimistic locking check
        if (updated.version > 0 && updated.version != existing.version) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(existing)
                    .build();
        }

        existing.color = updated.color;
        existing.fillColor = updated.fillColor;
        existing.strokeWidth = updated.strokeWidth;
        existing.opacity = updated.opacity;
        existing.geometryData = updated.geometryData;
        existing.text = updated.text;
        existing.metadata = updated.metadata;
        existing.status = updated.status != null ? updated.status : existing.status;
        existing.updatedBy = updated.updatedBy;
        existing.updatedAt = Instant.now();
        existing.version = existing.version + 1;

        return Response.ok(existing).build();
    }

    @POST
    @Path("/batch")
    @Transactional
    public Response batchUpsert(AnnotationBatchRequest request) {
        AnnotationBatchResponse response = new AnnotationBatchResponse();
        if (request == null || request.annotations == null) {
            response.success = false;
            response.message = "Empty batch request";
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        for (MarkupEntity incoming : request.annotations) {
            if (incoming.id == null || incoming.id.isEmpty()) {
                incoming.id = UUID.randomUUID().toString();
            }

            MarkupEntity existing = MarkupEntity.findById(incoming.id);
            if (existing == null) {
                // New annotation
                incoming.createdAt = Instant.now();
                incoming.updatedAt = Instant.now();
                incoming.version = 1;
                incoming.deleted = false;
                if (incoming.status == null) incoming.status = "Open";
                incoming.persist();
                response.synced.add(incoming);
            } else {
                // Check optimistic versioning
                if (incoming.version < existing.version) {
                    // Conflict: incoming is older than server version
                    response.conflicts.add(existing);
                } else {
                    // Overwrite with newer or equal client version (Last-Write-Wins with version increment)
                    existing.color = incoming.color;
                    existing.fillColor = incoming.fillColor;
                    existing.strokeWidth = incoming.strokeWidth;
                    existing.opacity = incoming.opacity;
                    existing.geometryData = incoming.geometryData;
                    existing.text = incoming.text;
                    existing.metadata = incoming.metadata;
                    existing.layer = incoming.layer != null ? incoming.layer : existing.layer;
                    existing.status = incoming.status != null ? incoming.status : existing.status;
                    existing.deleted = incoming.deleted;
                    existing.updatedBy = incoming.updatedBy;
                    existing.updatedAt = Instant.now();
                    existing.version = existing.version + 1;
                    response.synced.add(existing);
                }
            }
        }

        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{id}/status")
    @Transactional
    public Response updateStatus(@PathParam("id") String id, AnnotationStatusUpdateDto dto) {
        MarkupEntity existing = MarkupEntity.findById(id);
        if (existing == null || existing.deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (dto != null && dto.status != null) {
            existing.status = dto.status;
            existing.updatedBy = dto.updatedBy;
            existing.updatedAt = Instant.now();
            existing.version = existing.version + 1;
        }
        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteMarkup(@PathParam("id") String id) {
        MarkupEntity existing = MarkupEntity.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        existing.deleted = true;
        existing.updatedAt = Instant.now();
        existing.version = existing.version + 1;
        return Response.noContent().build();
    }

    // --- Annotation Comments ---

    @GET
    @Path("/{id}/comments")
    public List<AnnotationCommentEntity> getComments(@PathParam("id") String id) {
        return AnnotationCommentEntity.list("markupId = ?1 order by createdAt asc", id);
    }

    @POST
    @Path("/{id}/comments")
    @Transactional
    public Response addComment(@PathParam("id") String id, AnnotationCommentDto dto) {
        MarkupEntity existing = MarkupEntity.findById(id);
        if (existing == null || existing.deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        AnnotationCommentEntity comment = new AnnotationCommentEntity();
        comment.id = UUID.randomUUID().toString();
        comment.markupId = id;
        comment.authorId = dto.authorId;
        comment.authorName = dto.authorName != null ? dto.authorName : "Field Engineer";
        comment.text = dto.text;
        comment.createdAt = Instant.now();
        comment.persist();

        return Response.status(Response.Status.CREATED).entity(comment).build();
    }
}
