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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerEventBundleServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.setUpMetadata("tracker/event_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testCreateSingleEventData() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_events_and_enrollment.json");
    assertEquals(8, trackerObjects.getEvents().size());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    List<Event> events = manager.getAll(Event.class);
    assertEquals(8, events.size());
  }

  @Test
  void testUpdateSingleEventData() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_events_and_enrollment.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertEquals(8, manager.getAll(Event.class).size());

    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    assertEquals(8, manager.getAll(Event.class).size());
  }
}
