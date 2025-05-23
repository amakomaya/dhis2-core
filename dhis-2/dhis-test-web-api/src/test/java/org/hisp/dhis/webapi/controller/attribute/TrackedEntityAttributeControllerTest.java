/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller.attribute;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.TestUtils;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luca Cambi
 */
@Transactional
class TrackedEntityAttributeControllerTest extends H2ControllerIntegrationTestBase {
  @Test
  void shouldGenerateRandomValuesOrgUnitCodeAndRandom() throws Exception {

    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('A');
    trackedEntityAttribute.setGenerated(true);

    String pattern = "ORG_UNIT_CODE() + RANDOM(#######)";

    TextPattern textPattern = TextPatternParser.parse(pattern);

    textPattern.setOwnerObject(Objects.fromClass(trackedEntityAttribute.getClass()));
    textPattern.setOwnerUid(trackedEntityAttribute.getUid());

    trackedEntityAttribute.setTextPattern(textPattern);
    trackedEntityAttribute.setPattern(pattern);

    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT,
                new String(TestUtils.convertObjectToJsonBytes(trackedEntityAttribute))));

    assertStatus(
        HttpStatus.OK,
        GET(
            TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT
                + "/"
                + uid
                + "/generateAndReserve"
                + "?ORG_UNIT_CODE=A030101"));
  }
}
