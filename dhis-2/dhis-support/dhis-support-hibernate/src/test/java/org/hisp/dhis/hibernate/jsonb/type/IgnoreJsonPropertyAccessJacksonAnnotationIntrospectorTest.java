/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.hibernate.jsonb.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class IgnoreJsonPropertyAccessJacksonAnnotationIntrospectorTest {

  public static class ClassUnderTest {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String writeOnlyProperty;

    @JsonProperty public String readAndWriteProperty;
  }

  @Test
  void testFindPropertyAccess() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setAnnotationIntrospector(
        new IgnoreJsonPropertyWriteOnlyAccessJacksonAnnotationIntrospector());

    ClassUnderTest classUnderTest = new ClassUnderTest();
    classUnderTest.writeOnlyProperty = "Foo";
    classUnderTest.readAndWriteProperty = "Bar";

    String json = objectMapper.writeValueAsString(classUnderTest);

    assertEquals("{\"writeOnlyProperty\":\"Foo\",\"readAndWriteProperty\":\"Bar\"}", json);
    ClassUnderTest newClassUnderTest = objectMapper.readValue(json, ClassUnderTest.class);
    assertEquals("Foo", newClassUnderTest.writeOnlyProperty);
    assertEquals("Bar", newClassUnderTest.readAndWriteProperty);
  }
}
