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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClient.ContentType;
import static org.hisp.dhis.webapi.Assertions.assertWebMessage;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link FileController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class FileControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testPostCustomScript() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Custom script created",
        POST("/files/script", Body("var i = 1;"), ContentType("application/javascript"))
            .content(HttpStatus.OK));
  }

  @Test
  void testPostCustomStyle() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Custom style created",
        POST("/files/style", Body("body { width: 100%; }"), ContentType("text/css"))
            .content(HttpStatus.OK));
  }
}
