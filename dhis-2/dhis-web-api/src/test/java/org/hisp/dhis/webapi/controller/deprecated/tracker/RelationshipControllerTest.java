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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.hisp.dhis.dxf2.deprecated.tracker.relationship.RelationshipService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class RelationshipControllerTest {
  private MockMvc mockMvc;

  private static final String TEI_ID = "TEI_ID";

  private static final String EVENT_ID = "EVENT_ID";

  private static final String ENROLLMENT_ID = "ENROLLMENT_ID";

  private static final String REL_ID = "REL_ID";

  private TrackedEntity tei = new TrackedEntity();

  private Enrollment enrollment = new Enrollment();

  private Event event = new Event();

  private Relationship relationship = new Relationship();

  @Mock private RelationshipService relationshipService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private EnrollmentService enrollmentService;

  @Mock private EventService eventService;

  @InjectMocks private RelationshipController relationshipController;

  private static final String ENDPOINT = "/relationships";

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(relationshipController).build();
  }

  @Test
  void verifyEndpointWithNoArgs() {
    assertThrows(NestedServletException.class, () -> mockMvc.perform(get(ENDPOINT)));
  }

  @Test
  void verifyEndpointWithNotFoundTei() {
    assertThrows(
        NestedServletException.class, () -> mockMvc.perform(get(ENDPOINT).param("tei", TEI_ID)));
  }

  @Test
  void verifyEndpointWithTei() throws Exception {
    when(trackedEntityService.getTrackedEntity(TEI_ID)).thenReturn(tei);
    mockMvc.perform(get(ENDPOINT).param("tei", TEI_ID)).andExpect(status().isOk());

    verify(trackedEntityService).getTrackedEntity(TEI_ID);
    verify(relationshipService)
        .getRelationshipsByTrackedEntityInstance(eq(tei), any(), eq(false), eq(false));
  }

  @Test
  void verifyEndpointWithNotFoundEvent() {
    assertThrows(
        NestedServletException.class,
        () -> mockMvc.perform(get(ENDPOINT).param("event", EVENT_ID)));
  }

  @Test
  void verifyEndpointWithEvent() throws Exception {
    when(eventService.getEvent(EVENT_ID)).thenReturn(event);
    mockMvc.perform(get(ENDPOINT).param("event", EVENT_ID)).andExpect(status().isOk());

    verify(eventService).getEvent(EVENT_ID);
    verify(relationshipService).getRelationshipsByEvent(eq(event), any(), eq(false), eq(false));
  }

  @Test
  void verifyEndpointWithNotFoundEnrollment() {
    assertThrows(
        NestedServletException.class,
        () -> mockMvc.perform(get(ENDPOINT).param("enrollment", ENROLLMENT_ID)));
  }

  @Test
  void verifyEndpointWithEnrollment() throws Exception {
    when(enrollmentService.getEnrollment(ENROLLMENT_ID)).thenReturn(enrollment);
    mockMvc.perform(get(ENDPOINT).param("enrollment", ENROLLMENT_ID)).andExpect(status().isOk());

    verify(enrollmentService).getEnrollment(ENROLLMENT_ID);
    verify(relationshipService)
        .getRelationshipsByEnrollment(eq(enrollment), any(), eq(false), eq(false));
  }

  @Test
  void testGetRelationshipNotPresent() {
    assertThrows(NestedServletException.class, () -> mockMvc.perform(get(ENDPOINT + "/" + REL_ID)));
  }

  @Test
  void testGetRelationship() throws Exception {
    when(relationshipService.findRelationshipByUid(REL_ID)).thenReturn(Optional.of(relationship));
    mockMvc.perform(get(ENDPOINT + "/" + REL_ID)).andExpect(status().isOk());
  }

  @Test
  void testDeleteRelationship() throws Exception {
    when(relationshipService.findRelationshipByUid(REL_ID)).thenReturn(Optional.of(relationship));
    mockMvc.perform(get(ENDPOINT + "/" + REL_ID)).andExpect(status().isOk());
  }
}
