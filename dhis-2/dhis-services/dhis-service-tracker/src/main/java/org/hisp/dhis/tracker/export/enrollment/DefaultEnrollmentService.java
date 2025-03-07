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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service("org.hisp.dhis.tracker.export.enrollment.EnrollmentService")
class DefaultEnrollmentService implements EnrollmentService {
  private final HibernateEnrollmentStore enrollmentStore;

  private final EventService eventService;

  private final RelationshipService relationshipService;

  private final TrackerOwnershipManager trackerOwnershipAccessManager;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackerAccessManager trackerAccessManager;

  private final EnrollmentOperationParamsMapper paramsMapper;

  @Nonnull
  @Override
  public Optional<Enrollment> findEnrollment(@Nonnull UID uid) {
    try {
      return Optional.of(getEnrollment(uid));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  public Enrollment getEnrollment(@Nonnull UID uid) throws NotFoundException {
    return getEnrollment(uid, EnrollmentParams.FALSE);
  }

  @Nonnull
  @Override
  public Enrollment getEnrollment(@Nonnull UID uid, @Nonnull EnrollmentParams params)
      throws NotFoundException {
    Page<Enrollment> enrollments;
    try {
      EnrollmentOperationParams operationParams =
          EnrollmentOperationParams.builder()
              .enrollments(Set.of(uid))
              .enrollmentParams(params)
              .build();
      enrollments = findEnrollments(operationParams, PageParams.single());
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EnrollmentOperationParams are built");
    }

    if (enrollments.getItems().isEmpty()) {
      throw new NotFoundException(Enrollment.class, uid);
    }

    return enrollments.getItems().get(0);
  }

  @Nonnull
  @Override
  public List<Enrollment> findEnrollments(@Nonnull Set<UID> uids) throws ForbiddenException {
    if (uids.isEmpty()) {
      return List.of();
    }

    try {
      return findEnrollments(EnrollmentOperationParams.builder().enrollments(uids).build());
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EnrollmentOperationParams are built");
    }
  }

  @Nonnull
  @Override
  public List<Enrollment> findEnrollments(@Nonnull EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    return findEnrollments(
        new ArrayList<>(enrollmentStore.getEnrollments(queryParams)),
        params.getEnrollmentParams(),
        params.isIncludeDeleted(),
        queryParams.getOrganisationUnitMode());
  }

  @Nonnull
  @Override
  public Page<Enrollment> findEnrollments(
      @Nonnull EnrollmentOperationParams params, PageParams pageParams)
      throws ForbiddenException, BadRequestException {
    EnrollmentQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());

    Page<Enrollment> enrollmentsPage = enrollmentStore.getEnrollments(queryParams, pageParams);
    List<Enrollment> enrollments =
        findEnrollments(
            enrollmentsPage.getItems(),
            params.getEnrollmentParams(),
            params.isIncludeDeleted(),
            queryParams.getOrganisationUnitMode());
    return enrollmentsPage.withFilteredItems(enrollments);
  }

  private Set<Event> getEvents(
      Enrollment enrollment, EventParams eventParams, boolean includeDeleted) {
    EventOperationParams eventOperationParams =
        EventOperationParams.builder()
            .enrollments(Set.of(UID.of(enrollment)))
            .eventParams(eventParams)
            .includeDeleted(includeDeleted)
            .build();
    try {
      return Set.copyOf(eventService.findEvents(eventOperationParams));
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    } catch (ForbiddenException e) {
      // ForbiddenExceptions are caused when mapping the EventOperationParams.
      // These params are not present in the EventOperationParams created in this method.
      // Other reasons the user does not have access to data will
      // not be shown as such items are simply not returned in collections.
      return Set.of();
    }
  }

  private Enrollment getEnrollment(
      @Nonnull Enrollment enrollment, @Nonnull EnrollmentParams params, boolean includeDeleted) {
    Enrollment result = new Enrollment();
    result.setUid(enrollment.getUid());

    if (enrollment.getTrackedEntity() != null) {
      TrackedEntity trackedEntity = new TrackedEntity();
      trackedEntity.setUid(enrollment.getTrackedEntity().getUid());
      result.setTrackedEntity(trackedEntity);
    }
    OrganisationUnit organisationUnit = new OrganisationUnit();
    organisationUnit.setUid(enrollment.getOrganisationUnit().getUid());
    result.setOrganisationUnit(organisationUnit);
    result.setGeometry(enrollment.getGeometry());
    result.setCreated(enrollment.getCreated());
    result.setCreatedAtClient(enrollment.getCreatedAtClient());
    result.setLastUpdated(enrollment.getLastUpdated());
    result.setLastUpdatedAtClient(enrollment.getLastUpdatedAtClient());
    result.setProgram(enrollment.getProgram());
    result.setStatus(enrollment.getStatus());
    result.setEnrollmentDate(enrollment.getEnrollmentDate());
    result.setOccurredDate(enrollment.getOccurredDate());
    result.setFollowup(enrollment.getFollowup());
    result.setCompletedDate(enrollment.getCompletedDate());
    result.setCompletedBy(enrollment.getCompletedBy());
    result.setStoredBy(enrollment.getStoredBy());
    result.setCreatedByUserInfo(enrollment.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(enrollment.getLastUpdatedByUserInfo());
    result.setDeleted(enrollment.isDeleted());
    result.setNotes(enrollment.getNotes());
    if (params.isIncludeEvents()) {
      result.setEvents(
          getEvents(
              enrollment, params.getEnrollmentEventsParams().getEventParams(), includeDeleted));
    }
    if (params.isIncludeRelationships()) {
      result.setRelationshipItems(
          relationshipService.findRelationshipItems(
              TrackerType.ENROLLMENT, UID.of(result), includeDeleted));
    }
    if (params.isIncludeAttributes()) {
      result
          .getTrackedEntity()
          .setTrackedEntityAttributeValues(getTrackedEntityAttributeValues(enrollment));
    }

    return result;
  }

  private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(Enrollment enrollment) {
    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes(
            List.of(enrollment.getProgram()), null);
    Set<TrackedEntityAttributeValue> attributeValues = new LinkedHashSet<>();

    for (TrackedEntityAttributeValue trackedEntityAttributeValue :
        enrollment.getTrackedEntity().getTrackedEntityAttributeValues()) {
      if (readableAttributes.contains(trackedEntityAttributeValue.getAttribute())) {
        attributeValues.add(trackedEntityAttributeValue);
      }
    }

    return attributeValues;
  }

  private List<Enrollment> findEnrollments(
      Iterable<Enrollment> enrollments,
      EnrollmentParams params,
      boolean includeDeleted,
      OrganisationUnitSelectionMode orgUnitMode) {
    List<Enrollment> enrollmentList = new ArrayList<>();
    UserDetails currentUser = getCurrentUserDetails();

    for (Enrollment enrollment : enrollments) {
      if (enrollment != null
          && (orgUnitMode == ALL
              || trackerOwnershipAccessManager.hasAccess(
                  currentUser, enrollment.getTrackedEntity(), enrollment.getProgram()))
          && trackerAccessManager.canRead(currentUser, enrollment, orgUnitMode == ALL).isEmpty()) {
        enrollmentList.add(getEnrollment(enrollment, params, includeDeleted));
      }
    }

    return enrollmentList;
  }

  @Override
  public Set<String> getOrderableFields() {
    return enrollmentStore.getOrderableFields();
  }
}
