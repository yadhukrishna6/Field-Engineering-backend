package com.fieldengineering;

import com.fieldengineering.domain.MarkupEntity;
import com.fieldengineering.dto.AnnotationBatchRequest;
import com.fieldengineering.dto.AnnotationCommentDto;
import com.fieldengineering.dto.AnnotationStatusUpdateDto;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class MarkupResourceTest {

    @Test
    public void testMarkupLifecycleAndBatchSync() {
        String testDrawingId = "dwg-test-" + UUID.randomUUID();
        String testRevisionId = "rev-test-" + UUID.randomUUID();

        // 1. Create a Markup Annotation
        MarkupEntity markup = new MarkupEntity();
        markup.id = "mk-" + UUID.randomUUID();
        markup.drawingId = testDrawingId;
        markup.revisionId = testRevisionId;
        markup.pageNumber = 1;
        markup.type = "pen";
        markup.color = 0xFFFF6B00;
        markup.strokeWidth = 3.0;
        markup.geometryData = "[{\"x\":0.1,\"y\":0.2},{\"x\":0.3,\"y\":0.4}]";
        markup.createdBy = "test-engineer@field.com";
        markup.status = "Open";

        given()
                .contentType(ContentType.JSON)
                .body(markup)
                .when()
                .post("/api/v1/markups")
                .then()
                .statusCode(201)
                .body("id", equalTo(markup.id))
                .body("status", equalTo("Open"));

        // 2. List Markups for Drawing
        given()
                .when()
                .get("/api/v1/markups?drawingId=" + testDrawingId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // 3. Update Status
        AnnotationStatusUpdateDto statusDto = new AnnotationStatusUpdateDto();
        statusDto.status = "Addressed";
        statusDto.updatedBy = "reviewer@client.com";

        given()
                .contentType(ContentType.JSON)
                .body(statusDto)
                .when()
                .patch("/api/v1/markups/" + markup.id + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("Addressed"));

        // 4. Add Comment
        AnnotationCommentDto commentDto = new AnnotationCommentDto();
        commentDto.text = "Please verify flange thickness against ASME B16.5";
        commentDto.authorName = "Lead QA Engineer";

        given()
                .contentType(ContentType.JSON)
                .body(commentDto)
                .when()
                .post("/api/v1/markups/" + markup.id + "/comments")
                .then()
                .statusCode(201)
                .body("text", containsString("ASME B16.5"));

        // 5. Batch Upsert
        MarkupEntity batchMarkup = new MarkupEntity();
        batchMarkup.id = "mk-batch-" + UUID.randomUUID();
        batchMarkup.drawingId = testDrawingId;
        batchMarkup.revisionId = testRevisionId;
        batchMarkup.pageNumber = 1;
        batchMarkup.type = "text";
        batchMarkup.text = "P&ID TIE-IN 101";
        batchMarkup.geometryData = "[{\"x\":0.5,\"y\":0.5}]";
        batchMarkup.createdBy = "field-engineer@oilfield.com";

        AnnotationBatchRequest batchRequest = new AnnotationBatchRequest();
        batchRequest.annotations = Collections.singletonList(batchMarkup);

        given()
                .contentType(ContentType.JSON)
                .body(batchRequest)
                .when()
                .post("/api/v1/revisions/" + testRevisionId + "/annotations/batch")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("synced.size()", equalTo(1));
    }
}
