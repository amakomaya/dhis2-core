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
package org.hisp.dhis.test.e2e;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestRunStorage {
  private static LinkedHashMap<String, String> createdEntities;

  public static void addCreatedEntity(final String resource, final String id) {
    if (createdEntities == null) {
      createdEntities = new LinkedHashMap<>();
    }

    createdEntities.put(id, resource);
  }

  public static Map<String, String> getCreatedEntities() {
    if (createdEntities == null) {
      return new LinkedHashMap<>();
    }

    return new LinkedHashMap<>(createdEntities);
  }

  public static List<String> getCreatedEntities(String resource) {
    if (createdEntities == null) {
      return new ArrayList<>();
    }

    return getCreatedEntities().entrySet().stream()
        .filter(entrySet -> resource.equals(entrySet.getValue()))
        .map(Entry::getKey)
        .collect(toList());
  }

  public static void removeEntity(final String resource, final String id) {
    if (createdEntities == null) {
      return;
    }

    createdEntities.remove(id, resource);
  }

  public static void removeEntities(final String resource) {
    if (createdEntities == null) {
      return;
    }

    createdEntities.entrySet().removeIf(p -> p.getValue().equalsIgnoreCase(resource));
  }

  public static void removeAllEntities() {
    if (createdEntities == null) {
      return;
    }

    createdEntities.clear();
  }
}
