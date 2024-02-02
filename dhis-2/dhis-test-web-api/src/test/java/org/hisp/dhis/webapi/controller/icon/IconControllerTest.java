/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.icon;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonIcon;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class IconControllerTest extends DhisControllerIntegrationTest {
  private static final String iconKey = "iconKey";

  private static final String description = "description";

  private static final String keywords = "[\"k1\",\"k2\"]";

  private static final String keyword = "[\"m1\"]";

  @Autowired private ContextService contextService;

  @Test
  void shouldCreateCustomIconWhenFileResourceExist() throws IOException {

    String key =
        """
    {'key': '%s','description': '%s','fileResourceUid': '%s','keywords': %s}"""
            .formatted(iconKey, description, createFileResource(), keywords);
    JsonWebMessage message =
        POST("/icons/", key).content(HttpStatus.CREATED).as(JsonWebMessage.class);

    assertEquals(String.format("Icon %s created", iconKey), message.getMessage());
  }

  @Test
  void shouldGetIconWhenIconKeyExists() throws IOException {
    String fileResourceId = createFileResource();
    createCustomIcon(fileResourceId, keywords);

    JsonObject response = GET(String.format("/icons/%s", iconKey)).content();

    assertEquals(iconKey, response.getString("key").string());
    assertEquals(description, response.getString("description").string());
    assertEquals(fileResourceId, response.getString("fileResourceUid").string());
    assertEquals(keywords, response.getArray("keywords").toString());
    assertEquals(getCurrentUser().getUid(), response.getString("userUid").string());
    assertEquals(
        String.format(contextService.getApiPath() + "/icons/%s/icon", iconKey),
        response.getString("href").string());
  }

  @Test
  void shouldUpdateIconWhenKeyExists() throws IOException {
    String updatedDescription = "updatedDescription";
    String updatedKeywords = "['new k1', 'new k2']";
    createCustomIcon(createFileResource(), keywords);

    JsonObject response =
        PUT(
                "/icons",
                "{'key':'"
                    + iconKey
                    + "', 'description':'"
                    + updatedDescription
                    + "', 'keywords':"
                    + updatedKeywords
                    + "}")
            .content();

    assertEquals(String.format("Icon %s updated", iconKey), response.getString("message").string());
  }

  @Test
  void shouldGetOnlyCustomIcons() throws IOException {
    String fileResourceId = createFileResource();
    createCustomIcon(fileResourceId, keywords);

    JsonList<JsonIcon> icons =
        GET("/icons?keywords=k1").content(HttpStatus.OK).asList(JsonIcon.class);

    assertCustomIcons(icons.get(0), keywords, fileResourceId);
  }

  @Test
  void shouldGetIconsWithSpecifiedKey() throws IOException {
    String fileResourceId = createFileResource();
    createCustomIcon(fileResourceId, keyword);

    JsonList<JsonIcon> icons =
        GET("/icons?keywords=m1").content(HttpStatus.OK).asList(JsonIcon.class);

    assertCustomIcons(icons.get(0), keyword, fileResourceId);
  }

  @Test
  void shouldDeleteIconWhenKeyExists() throws IOException {
    createCustomIcon(createFileResource(), keywords);

    JsonObject response = DELETE(String.format("/icons/%s", iconKey)).content();

    assertEquals(String.format("Icon %s deleted", iconKey), response.getString("message").string());
  }

  private String createCustomIcon(String fileResourceId, String keywords) {

    String key =
        """
    {'key': '%s','description': '%s','fileResourceUid': '%s','keywords': %s}"""
            .formatted(iconKey, description, fileResourceId, keywords);
    JsonWebMessage message =
        POST("/icons/", key).content(HttpStatus.CREATED).as(JsonWebMessage.class);

    return message.getMessage();
  }

  private String createFileResource() throws IOException {
    InputStream in = getClass().getResourceAsStream("/icon/test-image.png");
    MockMultipartFile image = new MockMultipartFile("file", "test-image.png", "image/png", in);

    HttpResponse response = POST_MULTIPART("/fileResources?domain=CUSTOM_ICON", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");

    return savedObject.getString("id").string();
  }

  private void assertCustomIcons(JsonIcon icon, String keywords, String fileResourceId) {

    String actualKey = icon.getString("key").string();
    String actualDescription = icon.getString("description").string();
    String actualFileResourceId = icon.getString("fileResourceUid").string();
    String actualKeywords = icon.getArray("keywords").toString();
    assertAll(
        () ->
            assertEquals(
                iconKey,
                actualKey,
                String.format("Expected IconKey was %s but found %s", iconKey, actualKey)),
        () ->
            assertEquals(
                description,
                actualDescription,
                String.format(
                    "Expected Description was %s but found %s", description, actualDescription)),
        () ->
            assertEquals(
                fileResourceId,
                actualFileResourceId,
                String.format(
                    "Expected FileResourceId was %s but found %s",
                    fileResourceId, actualFileResourceId)),
        () ->
            assertEquals(
                keywords,
                actualKeywords,
                String.format("Expected keywords were %s but found %s", keywords, actualKeywords)));
  }
}
