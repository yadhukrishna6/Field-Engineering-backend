package com.fieldengineering.resource;

import com.fieldengineering.domain.MarkupEntity;
import com.fieldengineering.domain.MarkupLayerEntity;
import com.fieldengineering.dto.AnnotationBatchRequest;
import com.fieldengineering.dto.AnnotationBatchResponse;
import io.quarkus.panache.common.Parameters;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/revisions/{revisionId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RevisionAnnotationResource {

    @Inject
    MarkupResource markupResource;

    @GET
    @Path("/annotations")
    public List<MarkupEntity> listRevisionAnnotations(
            @PathParam("revisionId") String revisionId,
            @QueryParam("page") Integer page,
            @QueryParam("layerId") String layerId,
            @QueryParam("author") String author,
            @QueryParam("status") String status,
            @QueryParam("since") String since) {
        return markupResource.listMarkups(null, revisionId, page, layerId, status, since);
    }

    @POST
    @Path("/annotations/batch")
    @Transactional
    public Response batchUpsertRevisionAnnotations(
            @PathParam("revisionId") String revisionId,
            AnnotationBatchRequest request) {
        if (request != null && request.annotations != null) {
            for (MarkupEntity annotation : request.annotations) {
                if (annotation.revisionId == null || annotation.revisionId.isEmpty()) {
                    annotation.revisionId = revisionId;
                }
            }
        }
        return markupResource.batchUpsert(request);
    }

    @GET
    @Path("/layers")
    public List<MarkupLayerEntity> listLayers(@PathParam("revisionId") String revisionId) {
        return MarkupLayerEntity.list("revisionId = ?1 order by createdAt asc", revisionId);
    }

    @POST
    @Path("/layers")
    @Transactional
    public Response createLayer(@PathParam("revisionId") String revisionId, MarkupLayerEntity layer) {
        if (layer.id == null || layer.id.isEmpty()) {
            layer.id = UUID.randomUUID().toString();
        }
        layer.revisionId = revisionId;
        layer.createdAt = Instant.now();
        layer.updatedAt = Instant.now();
        layer.persist();
        return Response.status(Response.Status.CREATED).entity(layer).build();
    }
}
