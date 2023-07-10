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
package org.hisp.dhis.dxf2.deprecated.tracker.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Attribute;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.EnrollmentStore;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 * @deprecated this is a class related to "old" (deprecated) tracker which will be removed with
 *     "old" tracker. Make sure to plan migrating to new tracker.
 */
@Component
@RequiredArgsConstructor
@Deprecated(since = "2.41")
public class EnrollmentAggregate extends AbstractAggregate {
  @Nonnull private final EnrollmentStore enrollmentStore;

  @Nonnull private final EventAggregate eventAggregate;

  /**
   * Key: tei uid , value Enrollment
   *
   * @param ids a List of {@see TrackedEntity} Primary Keys
   * @return a MultiMap where key is a {@see TrackedEntity} uid and the key a List of {@see
   *     Enrollment} objects
   */
  Multimap<String, Enrollment> findByTrackedEntityInstanceIds(
      List<Long> ids, AggregateContext ctx) {
    Multimap<String, Enrollment> enrollments =
        enrollmentStore.getEnrollmentsByTrackedEntityInstanceIds(ids, ctx);

    if (enrollments.isEmpty()) {
      return enrollments;
    }

    List<Long> enrollmentIds =
        enrollments.values().stream().map(Enrollment::getId).collect(Collectors.toList());

    final CompletableFuture<Multimap<String, Event>> eventAsync =
        conditionalAsyncFetch(
            ctx.getParams().getEnrollmentParams().isIncludeEvents(),
            () -> eventAggregate.findByEnrollmentIds(enrollmentIds, ctx),
            ThreadPoolManager.getPool());

    final CompletableFuture<Multimap<String, Relationship>> relationshipAsync =
        conditionalAsyncFetch(
            ctx.getParams().getEnrollmentParams().isIncludeRelationships(),
            () -> enrollmentStore.getRelationships(enrollmentIds, ctx),
            ThreadPoolManager.getPool());

    final CompletableFuture<Multimap<String, Note>> notesAsync =
        asyncFetch(() -> enrollmentStore.getNotes(enrollmentIds), ThreadPoolManager.getPool());

    final CompletableFuture<Multimap<String, Attribute>> attributesAsync =
        conditionalAsyncFetch(
            ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes(),
            () -> enrollmentStore.getAttributes(enrollmentIds, ctx),
            ThreadPoolManager.getPool());

    return allOf(eventAsync, notesAsync, relationshipAsync, attributesAsync)
        .thenApplyAsync(
            fn -> {
              Multimap<String, Event> events = eventAsync.join();
              Multimap<String, Note> notes = notesAsync.join();
              Multimap<String, Relationship> relationships = relationshipAsync.join();
              Multimap<String, Attribute> attributes = attributesAsync.join();

              for (Enrollment enrollment : enrollments.values()) {
                if (ctx.getParams().getTeiEnrollmentParams().isIncludeEvents()) {
                  enrollment.setEvents(new ArrayList<>(events.get(enrollment.getEnrollment())));
                }
                if (ctx.getParams().getTeiEnrollmentParams().isIncludeRelationships()) {
                  enrollment.setRelationships(
                      new HashSet<>(relationships.get(enrollment.getEnrollment())));
                }
                if (ctx.getParams().getTeiEnrollmentParams().isIncludeAttributes()) {
                  enrollment.setAttributes(
                      new ArrayList<>(attributes.get(enrollment.getEnrollment())));
                }

                enrollment.setNotes(new ArrayList<>(notes.get(enrollment.getEnrollment())));
              }

              return enrollments;
            },
            ThreadPoolManager.getPool())
        .join();
  }
}
