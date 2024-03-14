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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.TrackedEntityAggregate;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService")
@RequiredArgsConstructor
class DefaultTrackedEntityService implements TrackedEntityService {

  private final TrackedEntityStore trackedEntityStore;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityChangeLogService trackedEntityChangeLogService;

  private final TrackerAccessManager trackerAccessManager;

  private final TrackedEntityAggregate trackedEntityAggregate;

  private final ProgramService programService;

  private final EnrollmentService enrollmentService;

  private final EventService eventService;

  private final FileResourceService fileResourceService;

  private final TrackedEntityOperationParamsMapper mapper;

  private final UserService userService;

  @Override
  public FileResourceStream getFileResource(
      UID trackedEntity, UID attribute, @CheckForNull UID program) throws NotFoundException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.of(fileResourceService, fileResource);
  }

  @Override
  public FileResourceStream getFileResourceImage(
      UID trackedEntity, UID attribute, @CheckForNull UID program, ImageFileDimension dimension)
      throws NotFoundException {
    FileResource fileResource = getFileResourceMetadata(trackedEntity, attribute, program);
    return FileResourceStream.ofImage(fileResourceService, fileResource, dimension);
  }

  private FileResource getFileResourceMetadata(
      UID trackedEntityUid, UID attributeUid, @CheckForNull UID programUid)
      throws NotFoundException {
    TrackedEntity trackedEntity = trackedEntityStore.getByUid(trackedEntityUid.getValue());
    if (trackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, trackedEntityUid.getValue());
    }

    TrackedEntityAttribute attribute = getAttribute(attributeUid, trackedEntity, programUid);
    if (!attribute.getValueType().isFile()) {
      throw new NotFoundException(
          "Tracked entity attribute " + attributeUid.getValue() + " is not a file (or image).");
    }

    String fileResourceUid = null;
    for (TrackedEntityAttributeValue attributeValue :
        trackedEntity.getTrackedEntityAttributeValues()) {
      if (attributeUid.getValue().equals(attributeValue.getAttribute().getUid())) {
        fileResourceUid = attributeValue.getValue();
        break;
      }
    }

    if (fileResourceUid == null) {
      throw new NotFoundException(
          "Attribute value for tracked entity attribute "
              + attributeUid.getValue()
              + " could not be found.");
    }

    return fileResourceService.getExistingFileResource(fileResourceUid);
  }

  /**
   * Tracked entity attributes are fetched from the program if supplied, otherwise from the tracked
   * entities type. Access is determined through the program sharing and ownership if present. If no
   * program is supplied, we fall back to tracked entity type ownership and the registering org unit
   * of the tracked entity.
   */
  private TrackedEntityAttribute getAttribute(
      UID attributeUid, TrackedEntity trackedEntity, @CheckForNull UID programUid)
      throws NotFoundException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (programUid != null) {
      Program program = programService.getProgram(programUid.getValue());
      if (program == null) {
        throw new NotFoundException(Program.class, programUid.getValue());
      }

      if (!trackerAccessManager.canRead(currentUser, trackedEntity, program, false).isEmpty()) {
        throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
      }
      return getAttribute(attributeUid, program);
    }

    if (!trackerAccessManager.canRead(currentUser, trackedEntity).isEmpty()) {
      throw new NotFoundException(TrackedEntity.class, trackedEntity.getUid());
    }
    return getAttribute(attributeUid, trackedEntity.getTrackedEntityType());
  }

  private TrackedEntityAttribute getAttribute(UID attribute, Program program)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getProgramAttributes(program);
    return getAttribute(attributes, attribute);
  }

  private TrackedEntityAttribute getAttribute(UID attribute, TrackedEntityType trackedEntityType)
      throws NotFoundException {
    Set<TrackedEntityAttribute> attributes =
        trackedEntityAttributeService.getTrackedEntityTypeAttributes(trackedEntityType);
    return getAttribute(attributes, attribute);
  }

  private static TrackedEntityAttribute getAttribute(
      Set<TrackedEntityAttribute> attributes, UID attribute) throws NotFoundException {
    return attributes.stream()
        .filter(att -> attribute.getValue().equals(att.getUid()))
        .findFirst()
        .orElseThrow(
            () -> new NotFoundException(TrackedEntityAttribute.class, attribute.getValue()));
  }

  @Override
  public TrackedEntity getTrackedEntity(
      String uid, String programIdentifier, TrackedEntityParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    Program program = null;

    if (StringUtils.isNotEmpty(programIdentifier)) {
      program = programService.getProgram(programIdentifier);

      if (program == null) {
        throw new NotFoundException(Program.class, programIdentifier);
      }
    }

    TrackedEntity trackedEntity;
    if (program != null) {
      trackedEntity = getTrackedEntity(uid, program, params, includeDeleted);
      if (params.isIncludeProgramOwners()) {
        Set<TrackedEntityProgramOwner> filteredProgramOwners =
            trackedEntity.getProgramOwners().stream()
                .filter(te -> te.getProgram().getUid().equals(programIdentifier))
                .collect(Collectors.toSet());
        trackedEntity.setProgramOwners(filteredProgramOwners);
      }
    } else {
      // return only tracked entity type attributes
      trackedEntity = getTrackedEntity(uid, params, includeDeleted);
      TrackedEntityType trackedEntityType = trackedEntity.getTrackedEntityType();
      if (trackedEntityType != null) {
        Set<String> tetAttributes =
            trackedEntityType.getTrackedEntityAttributes().stream()
                .map(TrackedEntityAttribute::getUid)
                .collect(Collectors.toSet());
        Set<TrackedEntityAttributeValue> tetAttributeValues =
            trackedEntity.getTrackedEntityAttributeValues().stream()
                .filter(att -> tetAttributes.contains(att.getAttribute().getUid()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        trackedEntity.setTrackedEntityAttributeValues(tetAttributeValues);
      }
    }
    return trackedEntity;
  }

  @Override
  public TrackedEntity getTrackedEntity(
      String uid, TrackedEntityParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    return getTrackedEntity(uid, params, includeDeleted, trackerAccessManager::canRead);
  }

  @Override
  public TrackedEntity getTrackedEntity(
      String uid, Program program, TrackedEntityParams params, boolean includeDeleted)
      throws NotFoundException, ForbiddenException {
    return getTrackedEntity(
        uid,
        params,
        includeDeleted,
        (currentUser, daoTrackedEntity) ->
            trackerAccessManager.canRead(currentUser, daoTrackedEntity, program, false));
  }

  private TrackedEntity getTrackedEntity(
      String uid,
      TrackedEntityParams params,
      boolean includeDeleted,
      BiFunction<User, TrackedEntity, List<String>> aclCheckFunction)
      throws NotFoundException, ForbiddenException {
    TrackedEntity daoTrackedEntity = trackedEntityStore.getByUid(uid);
    addTrackedEntityAudit(daoTrackedEntity, CurrentUserUtil.getCurrentUsername());
    if (daoTrackedEntity == null) {
      throw new NotFoundException(TrackedEntity.class, uid);
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    List<String> errors = aclCheckFunction.apply(currentUser, daoTrackedEntity);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    return mapTrackedEntity(daoTrackedEntity, params, currentUser, includeDeleted);
  }

  private TrackedEntity mapTrackedEntity(
      TrackedEntity trackedEntity,
      TrackedEntityParams params,
      User currentUser,
      boolean includeDeleted) {
    TrackedEntity result = new TrackedEntity();
    result.setId(trackedEntity.getId());
    result.setUid(trackedEntity.getUid());
    result.setOrganisationUnit(trackedEntity.getOrganisationUnit());
    result.setTrackedEntityType(trackedEntity.getTrackedEntityType());
    result.setCreated(trackedEntity.getCreated());
    result.setCreatedAtClient(trackedEntity.getCreatedAtClient());
    result.setLastUpdated(trackedEntity.getLastUpdated());
    result.setLastUpdatedAtClient(trackedEntity.getLastUpdatedAtClient());
    result.setInactive(trackedEntity.isInactive());
    result.setGeometry(trackedEntity.getGeometry());
    result.setDeleted(trackedEntity.isDeleted());
    result.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
    result.setStoredBy(trackedEntity.getStoredBy());
    result.setCreatedByUserInfo(trackedEntity.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(trackedEntity.getLastUpdatedByUserInfo());
    result.setGeometry(trackedEntity.getGeometry());
    if (params.isIncludeRelationships()) {
      result.setRelationshipItems(getRelationshipItems(trackedEntity, currentUser, includeDeleted));
    }
    if (params.isIncludeEnrollments()) {
      result.setEnrollments(getEnrollments(trackedEntity, currentUser, includeDeleted));
    }
    if (params.isIncludeProgramOwners()) {
      result.setProgramOwners(trackedEntity.getProgramOwners());
    }
    result.setTrackedEntityAttributeValues(getTrackedEntityAttributeValues(trackedEntity));

    return result;
  }

  private Set<RelationshipItem> getRelationshipItems(
      TrackedEntity trackedEntity, User user, boolean includeDeleted) {
    Set<RelationshipItem> items = new HashSet<>();

    for (RelationshipItem relationshipItem : trackedEntity.getRelationshipItems()) {
      Relationship daoRelationship = relationshipItem.getRelationship();

      if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
          && (includeDeleted || !daoRelationship.isDeleted())) {
        items.add(relationshipItem);
      }
    }
    return items;
  }

  private Set<Enrollment> getEnrollments(
      TrackedEntity trackedEntity, User user, boolean includeDeleted) {
    Set<Enrollment> enrollments = new HashSet<>();

    for (Enrollment enrollment : trackedEntity.getEnrollments()) {
      if (trackerAccessManager.canRead(user, enrollment, false).isEmpty()
          && (includeDeleted || !enrollment.isDeleted())) {
        Set<Event> events = new HashSet<>();
        for (Event event : enrollment.getEvents()) {
          if (includeDeleted || !event.isDeleted()) {
            events.add(event);
          }
        }
        enrollment.setEvents(events);
        enrollments.add(enrollment);
      }
    }
    return enrollments;
  }

  private Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntity trackedEntity) {
    Set<TrackedEntityAttribute> readableAttributes =
        trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes();
    return trackedEntity.getTrackedEntityAttributeValues().stream()
        .filter(av -> readableAttributes.contains(av.getAttribute()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private RelationshipItem withNestedEntity(
      TrackedEntity trackedEntity, RelationshipItem item, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    // relationships of relationship items are not mapped to JSON so there is no need to fetch them
    RelationshipItem result = new RelationshipItem();

    if (item.getTrackedEntity() != null) {
      if (trackedEntity.getUid().equals(item.getTrackedEntity().getUid())) {
        // only fetch the TE if we do not already have access to it. meaning the TE owns the item
        // this is just mapping the TE
        result.setTrackedEntity(trackedEntity);
      } else {
        result.setTrackedEntity(
            getTrackedEntity(
                item.getTrackedEntity().getUid(),
                TrackedEntityParams.TRUE.withIncludeRelationships(false),
                includeDeleted));
      }
    } else if (item.getEnrollment() != null) {
      result.setEnrollment(
          enrollmentService.getEnrollment(
              item.getEnrollment().getUid(),
              EnrollmentParams.TRUE.withIncludeRelationships(false),
              false));
    } else if (item.getEvent() != null) {
      result.setEvent(
          eventService.getEvent(
              item.getEvent().getUid(), EventParams.TRUE.withIncludeRelationships(false)));
    }

    return result;
  }

  @Override
  public List<TrackedEntity> getTrackedEntities(TrackedEntityOperationParams operationParams)
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityQueryParams queryParams = mapper.map(operationParams);
    final List<Long> ids = getTrackedEntityIds(queryParams);

    List<TrackedEntity> trackedEntities =
        this.trackedEntityAggregate.find(
            ids,
            operationParams.getTrackedEntityParams(),
            queryParams,
            operationParams.getOrgUnitMode());

    mapRelationshipItems(
        trackedEntities,
        operationParams.getTrackedEntityParams(),
        operationParams.isIncludeDeleted());

    addSearchAudit(trackedEntities, queryParams.getUser());

    return trackedEntities;
  }

  @Override
  public Page<TrackedEntity> getTrackedEntities(
      TrackedEntityOperationParams operationParams, PageParams pageParams)
      throws BadRequestException, ForbiddenException, NotFoundException {
    TrackedEntityQueryParams queryParams = mapper.map(operationParams);
    final Page<Long> ids = getTrackedEntityIds(queryParams, pageParams);

    List<TrackedEntity> trackedEntities =
        this.trackedEntityAggregate.find(
            ids.getItems(),
            operationParams.getTrackedEntityParams(),
            queryParams,
            operationParams.getOrgUnitMode());

    mapRelationshipItems(
        trackedEntities,
        operationParams.getTrackedEntityParams(),
        operationParams.isIncludeDeleted());

    addSearchAudit(trackedEntities, queryParams.getUser());

    return ids.withItems(trackedEntities);
  }

  public List<Long> getTrackedEntityIds(TrackedEntityQueryParams params) {
    return trackedEntityStore.getTrackedEntityIds(params);
  }

  public Page<Long> getTrackedEntityIds(TrackedEntityQueryParams params, PageParams pageParams) {
    return trackedEntityStore.getTrackedEntityIds(params, pageParams);
  }

  /**
   * We need to return the full models for relationship items (i.e. trackedEntity, enrollment and
   * event) in our API. The aggregate stores currently do not support that, so we need to fetch the
   * entities individually.
   */
  private void mapRelationshipItems(
      List<TrackedEntity> trackedEntities, TrackedEntityParams params, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    if (params.isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        mapRelationshipItems(trackedEntity, includeDeleted);
      }
    }
    if (params.getEnrollmentParams().isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        for (Enrollment enrollment : trackedEntity.getEnrollments()) {
          mapRelationshipItems(enrollment, trackedEntity, includeDeleted);
        }
      }
    }
    if (params.getEventParams().isIncludeRelationships()) {
      for (TrackedEntity trackedEntity : trackedEntities) {
        for (Enrollment enrollment : trackedEntity.getEnrollments()) {
          for (Event event : enrollment.getEvents()) {
            mapRelationshipItems(event, trackedEntity, includeDeleted);
          }
        }
      }
    }
  }

  private void mapRelationshipItems(TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : trackedEntity.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, trackedEntity, trackedEntity, includeDeleted));
    }

    trackedEntity.setRelationshipItems(result);
  }

  private void mapRelationshipItems(
      Enrollment enrollment, TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : enrollment.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, enrollment, trackedEntity, includeDeleted));
    }

    enrollment.setRelationshipItems(result);
  }

  private void mapRelationshipItems(
      Event event, TrackedEntity trackedEntity, boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Set<RelationshipItem> result = new HashSet<>();

    for (RelationshipItem item : event.getRelationshipItems()) {
      result.add(mapRelationshipItem(item, event, trackedEntity, includeDeleted));
    }

    event.setRelationshipItems(result);
  }

  private RelationshipItem mapRelationshipItem(
      RelationshipItem item,
      BaseIdentifiableObject itemOwner,
      TrackedEntity trackedEntity,
      boolean includeDeleted)
      throws ForbiddenException, NotFoundException {
    Relationship rel = item.getRelationship();
    RelationshipItem from = withNestedEntity(trackedEntity, rel.getFrom(), includeDeleted);
    from.setRelationship(rel);
    rel.setFrom(from);
    RelationshipItem to = withNestedEntity(trackedEntity, rel.getTo(), includeDeleted);
    to.setRelationship(rel);
    rel.setTo(to);

    if (rel.getFrom().getTrackedEntity() != null
        && itemOwner.getUid().equals(rel.getFrom().getTrackedEntity().getUid())) {
      return from;
    }

    return to;
  }

  private void addSearchAudit(List<TrackedEntity> trackedEntities, User user) {
    if (trackedEntities.isEmpty()) {
      return;
    }
    final String accessedBy =
        user != null ? user.getUsername() : CurrentUserUtil.getCurrentUsername();
    Map<String, TrackedEntityType> tetMap =
        trackedEntityTypeService.getAllTrackedEntityType().stream()
            .collect(Collectors.toMap(TrackedEntityType::getUid, t -> t));

    List<TrackedEntityChangeLog> auditable =
        trackedEntities.stream()
            .filter(Objects::nonNull)
            .filter(te -> te.getTrackedEntityType() != null)
            .filter(te -> tetMap.get(te.getTrackedEntityType().getUid()).isAllowAuditLog())
            .map(te -> new TrackedEntityChangeLog(te.getUid(), accessedBy, ChangeLogType.SEARCH))
            .toList();

    if (!auditable.isEmpty()) {
      trackedEntityChangeLogService.addTrackedEntityChangeLog(auditable);
    }
  }

  private void addTrackedEntityAudit(TrackedEntity trackedEntity, String username) {
    if (username != null
        && trackedEntity != null
        && trackedEntity.getTrackedEntityType() != null
        && trackedEntity.getTrackedEntityType().isAllowAuditLog()) {
      TrackedEntityChangeLog trackedEntityChangeLog =
          new TrackedEntityChangeLog(trackedEntity.getUid(), username, ChangeLogType.READ);
      trackedEntityChangeLogService.addTrackedEntityChangeLog(trackedEntityChangeLog);
    }
  }

  @Override
  public Set<String> getOrderableFields() {
    return trackedEntityStore.getOrderableFields();
  }
}
