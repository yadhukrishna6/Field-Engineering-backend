package com.fieldengineering;

import com.fieldengineering.dto.PageMarkupDto;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class DrawingMarkupTest {

    @Test
    public void testDrawingUploadAndMarkupLifecycle() throws IOException {
        // Create a temporary dummy PNG image file
        File tempImage = File.createTempFile("test_drawing_", ".png");
        tempImage.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempImage)) {
            fos.write(new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D });
        }

        // 1. Upload Drawing
        String drawingId = given()
                .multiPart("file", tempImage, "image/png")
                .multiPart("name", "P&ID Isometric Area 101")
                .when()
                .post("/api/v1/drawings")
                .then()
                .statusCode(201)
                .body("name", equalTo("P&ID Isometric Area 101"))
                .body("fileType", equalTo("IMAGE"))
                .body("pageCount", equalTo(1))
                .extract().path("id");

        // 2. List Drawings
        given()
                .when()
                .get("/api/v1/drawings")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // 3. Get Drawing by ID
        given()
                .when()
                .get("/api/v1/drawings/" + drawingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(drawingId));

        // 4. Stream Drawing File
        given()
                .when()
                .get("/api/v1/drawings/" + drawingId + "/file")
                .then()
                .statusCode(200);

        // 5. Get Initial Empty Markup
        given()
                .when()
                .get("/api/v1/drawings/" + drawingId + "/pages/1/markup")
                .then()
                .statusCode(200)
                .body("pageNumber", equalTo(1))
                .body("version", equalTo(1));

        // 6. Initial Save of Page Markup (Strokes Payload) -> version 1
        PageMarkupDto markupDto = new PageMarkupDto();
        markupDto.drawingId = drawingId;
        markupDto.pageNumber = 1;
        markupDto.payload = "{\"strokes\":[{\"points\":[{\"x\":0.1,\"y\":0.2},{\"x\":0.3,\"y\":0.4}],\"color\":4294926080,\"width\":4.0}]}";
        markupDto.version = 1;

        given()
                .contentType(ContentType.JSON)
                .body(markupDto)
                .when()
                .put("/api/v1/drawings/" + drawingId + "/pages/1/markup")
                .then()
                .statusCode(200)
                .body("version", equalTo(1))
                .body("payload", containsString("4294926080"));

        // 7. Update Page Markup with matching version 1 -> version becomes 2
        markupDto.payload = "{\"strokes\":[{\"points\":[{\"x\":0.1,\"y\":0.2}],\"color\":4294926080,\"width\":4.0}]}";
        markupDto.version = 1;

        given()
                .contentType(ContentType.JSON)
                .body(markupDto)
                .when()
                .put("/api/v1/drawings/" + drawingId + "/pages/1/markup")
                .then()
                .statusCode(200)
                .body("version", equalTo(2));

        // 8. Verify Optimistic Version Conflict (409) with stale version 1
        PageMarkupDto staleDto = new PageMarkupDto();
        staleDto.drawingId = drawingId;
        staleDto.pageNumber = 1;
        staleDto.payload = "{\"strokes\":[]}";
        staleDto.version = 1; // Stale version (server is now at 2)

        given()
                .contentType(ContentType.JSON)
                .body(staleDto)
                .when()
                .put("/api/v1/drawings/" + drawingId + "/pages/1/markup")
                .then()
                .statusCode(409)
                .body("serverVersion", equalTo(2));
    }
}
