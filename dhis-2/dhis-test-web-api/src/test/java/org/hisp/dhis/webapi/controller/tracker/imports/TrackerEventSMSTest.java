/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static java.lang.String.format;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.encodeSms;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.DeleteSmsSubmission;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.TrackerEventSmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.test.message.FakeMessageSender;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests tracker SMS
 *
 * <ul>
 *   <li>to delete an event via a {@link DeleteSmsSubmission} implemented via {@link
 *       org.hisp.dhis.tracker.imports.sms.DeleteEventSMSListener}
 *   <li>to create an event via a {@link TrackerEventSmsSubmission} implemented via {@link
 *       org.hisp.dhis.tracker.imports.sms.TrackerEventSMSListener}
 * </ul>
 *
 * It also tests parts of {@link org.hisp.dhis.webapi.controller.sms.SmsInboundController} and other
 * SMS classes in the SMS class hierarchy.
 */
class TrackerEventSMSTest extends PostgresControllerIntegrationTestBase {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  @Autowired private EventService eventService;

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired private FakeMessageSender messageSender;

  private CategoryOptionCombo coc;

  private OrganisationUnit orgUnit;

  private Program program;

  private ProgramStage programStage;

  private User user;

  private TrackedEntityType trackedEntityType;

  private DataElement de;

  @BeforeEach
  void setUp() {
    coc = categoryService.getDefaultCategoryOptionCombo();

    orgUnit = createOrganisationUnit('A');

    user = createUserWithAuth("tester", Authorities.toStringArray(Authorities.F_MOBILE_SETTINGS));
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    user.setPhoneNumber("7654321");
    userService.updateUser(user);

    orgUnit.getSharing().setOwner(user);
    manager.save(orgUnit, false);

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(user);
    program.getSharing().addUserAccess(fullAccess(user));
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(user);
    manager.save(de, false);

    programStage = createProgramStage('A', program);
    programStage.setFeatureType(FeatureType.POINT);
    programStage.getSharing().setOwner(user);
    programStage.getSharing().addUserAccess(fullAccess(user));
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, de, 1, false);
    programStage.setProgramStageDataElements(Set.of(programStageDataElement));
    manager.save(programStage, false);
  }

  @AfterEach
  void afterEach() {
    messageSender.clearMessages();
  }

  @Test
  void shouldDeleteEvent() throws SmsCompressionException {
    Event event = event(enrollment(trackedEntity()));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(event.getUid());

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
                    """
        {
        "text": "%s",
        "originator": "%s"
        }
        """,
                    text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertEquals(sms.getReceivedDate(), sms.getSentDate()),
        () -> assertEquals("default", sms.getGatewayId()),
        () -> {
          String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
          OutboundMessage expectedMessage =
              new OutboundMessage(null, expectedText, Set.of(originator));
          assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
        });
    assertThrows(NotFoundException.class, () -> eventService.getEvent(UID.of(event.getUid())));
  }

  @Test
  void shouldDeleteEventViaRequestParameters() throws SmsCompressionException {
    Event event = event(enrollment(trackedEntity()));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 2;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(event.getUid());

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();
    LocalDateTime receivedTime = LocalDateTime.of(2024, 9, 2, 10, 15, 30);

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound?message={message}&originator={originator}&receivedTime={receivedTime}",
                text,
                originator,
                receivedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertNotEquals(sms.getReceivedDate(), sms.getSentDate()),
        () ->
            assertEquals(
                receivedTime,
                sms.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
        () -> assertEquals("Unknown", sms.getGatewayId()),
        () -> {
          String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
          OutboundMessage expectedMessage =
              new OutboundMessage(null, expectedText, Set.of(originator));
          assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
        });
    assertThrows(NotFoundException.class, () -> eventService.getEvent(UID.of(event.getUid())));
  }

  @Test
  void shouldFailDeletingNonExistingEvent() throws SmsCompressionException {
    UID uid = UID.of(CodeGenerator.generateUid());
    assertThrows(NotFoundException.class, () -> eventService.getEvent(uid));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 3;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(uid.getValue());

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
                    """
    {
    "text": "%s",
    "originator": "%s"
    }
    """,
                    text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.FAILED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> {
          String expectedText = submissionId + ":" + SmsResponse.INVALID_EVENT.set(uid.getValue());
          OutboundMessage expectedMessage =
              new OutboundMessage(null, expectedText, Set.of(originator));
          assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
        });
  }

  @Test
  void shouldCreateEvent() throws SmsCompressionException, ForbiddenException, NotFoundException {
    Enrollment enrollment = enrollment(trackedEntity());

    TrackerEventSmsSubmission submission = new TrackerEventSmsSubmission();
    int submissionId = 4;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    String eventUid = CodeGenerator.generateUid();
    submission.setEvent(eventUid);
    submission.setOrgUnit(orgUnit.getUid());
    submission.setProgramStage(programStage.getUid());
    submission.setEnrollment(enrollment.getUid());
    submission.setAttributeOptionCombo(coc.getUid());
    submission.setEventStatus(SmsEventStatus.COMPLETED);
    submission.setEventDate(DateUtils.getDate(2024, 9, 2, 10, 15));
    submission.setDueDate(DateUtils.getDate(2024, 9, 3, 16, 23));
    submission.setCoordinates(new GeoPoint(48.8575f, 2.3514f));

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
                    """
    {
    "text": "%s",
    "originator": "%s"
    }
    """,
                    text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> {
          String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
          OutboundMessage expectedMessage =
              new OutboundMessage(null, expectedText, Set.of(originator));
          assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
        });
    assertDoesNotThrow(() -> eventService.getEvent(UID.of(eventUid)));
    Event actual = eventService.getEvent(UID.of(eventUid));
    assertAll(
        "created event",
        () -> assertEquals(eventUid, actual.getUid()),
        () -> assertEqualUids(submission.getEnrollment(), actual.getEnrollment()),
        () -> assertEqualUids(submission.getOrgUnit(), actual.getOrganisationUnit()),
        () -> assertEqualUids(submission.getProgramStage(), actual.getProgramStage()),
        () ->
            assertEqualUids(submission.getAttributeOptionCombo(), actual.getAttributeOptionCombo()),
        () -> assertEquals(user.getUsername(), actual.getStoredBy()),
        () -> assertEquals(submission.getEventDate(), actual.getOccurredDate()),
        () -> assertEquals(submission.getDueDate(), actual.getScheduledDate()),
        () -> assertEquals(EventStatus.COMPLETED, actual.getStatus()),
        () -> assertEquals(user.getUsername(), actual.getCompletedBy()),
        () -> assertNotNull(actual.getCompletedDate()),
        () -> assertGeometry(submission.getCoordinates(), actual.getGeometry()));
  }

  @Test
  void shouldUpdateEvent() throws SmsCompressionException, ForbiddenException, NotFoundException {
    Enrollment enrollment = enrollment(trackedEntity());
    Event event = event(enrollment);

    TrackerEventSmsSubmission submission = new TrackerEventSmsSubmission();
    int submissionId = 5;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(event.getUid());
    submission.setOrgUnit(event.getOrganisationUnit().getUid());
    submission.setProgramStage(event.getProgramStage().getUid());
    submission.setEnrollment(enrollment.getUid());
    submission.setAttributeOptionCombo(event.getAttributeOptionCombo().getUid());
    submission.setEventStatus(SmsEventStatus.COMPLETED);
    submission.setEventDate(event.getOccurredDate());
    submission.setDueDate(event.getScheduledDate());
    // The coc has to be set so the sms-compression library can encode the data value. Not sure why
    // that is necessary though.
    submission.setValues(List.of(new SmsDataValue(coc.getUid(), de.getUid(), "hello")));

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST("/sms/inbound", format("""
{
"text": "%s",
"originator": "%s"
}
""", text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> {
          String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
          OutboundMessage expectedMessage =
              new OutboundMessage(null, expectedText, Set.of(originator));
          assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
        });
    assertDoesNotThrow(() -> eventService.getEvent(UID.of(event.getUid())));
    Event actual = eventService.getEvent(UID.of(event.getUid()));
    assertAll(
        "updated event",
        () -> assertEqualUids(submission.getEnrollment(), actual.getEnrollment()),
        () -> assertEqualUids(submission.getOrgUnit(), actual.getOrganisationUnit()),
        () -> assertEqualUids(submission.getProgramStage(), actual.getProgramStage()),
        () ->
            assertEqualUids(submission.getAttributeOptionCombo(), actual.getAttributeOptionCombo()),
        () -> assertNull(actual.getStoredBy()),
        () -> assertEquals(event.getOccurredDate(), actual.getOccurredDate()),
        () -> assertEquals(event.getScheduledDate(), actual.getScheduledDate()),
        () -> assertEquals(EventStatus.COMPLETED, actual.getStatus()),
        () -> assertEquals(user.getUsername(), actual.getCompletedBy()),
        () -> assertNotNull(actual.getCompletedDate()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user.getUsername());
          assertContainsOnly(Set.of(expected), actual.getEventDataValues());
        },
        () -> assertNull(actual.getGeometry()));
  }

  private IncomingSms getSms(JsonWebMessage response) {
    assertStartsWith("Received SMS: ", response.getMessage());

    String smsUid = response.getMessage().replaceFirst("^Received SMS: ", "");
    IncomingSms sms = incomingSmsService.get(smsUid);
    assertNotNull(sms, "failed to find SMS in DB with UID " + smsUid);
    return sms;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(user);
    type.getSharing().addUserAccess(fullAccess(user));
    manager.save(type, false);
    return type;
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    return trackedEntity(orgUnit, trackedEntityType);
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(user);
    return te;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit(), coc);
    event.setOccurredDate(new Date());
    event.setAutoFields();
    manager.save(event);
    return event;
  }

  private TrackedEntityType trackedEntityType(char uniqueChar) {
    TrackedEntityType type = createTrackedEntityType(uniqueChar);
    type.getSharing().setOwner(user);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private UserAccess fullAccess(User user) {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private static void assertEqualUids(Uid expected, IdentifiableObject actual) {
    assertEquals(expected.getUid(), actual.getUid());
  }

  private static void assertGeometry(GeoPoint expected, Geometry actual) {
    assertEquals(
        new GeometryFactory()
            .createPoint(new Coordinate(expected.getLongitude(), expected.getLatitude())),
        actual);
  }
}
