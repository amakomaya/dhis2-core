/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;

import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OperationsParamsValidator {

  /**
   * Validates the user is authorized and/or has the necessary configuration set up in case the org
   * unit mode is ALL, ACCESSIBLE or CAPTURE. If the mode used is none of these three, no validation
   * will be run
   *
   * @param orgUnitMode the {@link OrganisationUnitSelectionMode orgUnitMode} used in the current
   *     case
   * @throws BadRequestException if a validation error occurs for any of the three aforementioned
   *     modes
   */
  public static void validateOrgUnitMode(
      OrganisationUnitSelectionMode orgUnitMode, User user, Program program)
      throws BadRequestException {

    switch (orgUnitMode) {
      case ALL -> validateUserCanSearchOrgUnitModeALL(user);
      case ACCESSIBLE, DESCENDANTS, CHILDREN -> validateAccessibleScope(user, program, orgUnitMode);
      case CAPTURE -> validateCaptureScope(user);
    }
  }

  private static void validateUserCanSearchOrgUnitModeALL(User user) throws BadRequestException {
    if (user == null
        || !(user.isSuper()
            || user.isAuthorized(
                Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()))) {
      throw new BadRequestException(
          "Current user is not authorized to query across all organisation units");
    }
  }

  private static void validateAccessibleScope(
      User user, Program program, OrganisationUnitSelectionMode orgUnitMode)
      throws BadRequestException {

    if (user == null) {
      throw new BadRequestException("User is required for orgUnitMode: " + orgUnitMode);
    }

    if (program != null && (program.isClosed() || program.isProtected())) {
      if (user.getOrganisationUnits().isEmpty()) {
        throw new BadRequestException("User needs to be assigned data capture orgunits");
      }

    } else if (user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()) {
      throw new BadRequestException(
          "User needs to be assigned either TE search, data view or data capture org units");
    }
  }

  private static void validateCaptureScope(User user) throws BadRequestException {

    if (user == null) {
      throw new BadRequestException("User is required for orgUnitMode: " + CAPTURE);
    } else if (user.getOrganisationUnits().isEmpty()) {
      throw new BadRequestException("User needs to be assigned data capture orgunits");
    }
  }

  /**
   * Returns a list of all the org units the user has access to
   *
   * @param user the user to check the access of
   * @param requestedOrgUnits parent org units to get descendants/children of
   * @param program the program the user wants to access to
   * @return a list containing the user accessible organisation units
   * @throws ForbiddenException if the user has no access to any of the provided org units
   */
  public static Set<OrganisationUnit> validateAccessibleOrgUnits(
      User user,
      Set<OrganisationUnit> requestedOrgUnits,
      OrganisationUnitSelectionMode orgUnitMode,
      Program program,
      TrackerAccessManager trackerAccessManager)
      throws ForbiddenException {

    Set<OrganisationUnit> accessibleOrgUnits = new HashSet<>();

    for (OrganisationUnit orgUnit : requestedOrgUnits) {
      OrganisationUnit accessibleOrgUnitFound =
          switch (orgUnitMode) {
            case DESCENDANTS -> getAccessibleDescendant(user, program, orgUnit, true);
            case CHILDREN -> getAccessibleDescendant(user, program, orgUnit, false);
            case SELECTED -> getSelectedOrgUnit(user, program, orgUnit, trackerAccessManager);
            default -> null;
          };

      if (accessibleOrgUnitFound == null) {
        throw new ForbiddenException(
            "User does not have access to orgUnit: " + (orgUnit != null ? orgUnit.getUid() : ""));
      }

      accessibleOrgUnits.add(accessibleOrgUnitFound);
    }

    if (orgUnitMode == CAPTURE) {
      return new HashSet<>(user.getOrganisationUnits());
    } else if (orgUnitMode == ACCESSIBLE) {
      return getAccessibleOrgUnits(user, program);
    }

    return accessibleOrgUnits;
  }

  /**
   * Returns the org unit whose path is contained in the user search or capture scope org unit. If
   * there's a match, it means the user org unit is at the same level or above the supplied org
   * unit.
   *
   * @param user the user to check the access of
   * @param program the program the user wants to access to
   * @return an org unit the user has access to
   */
  private static OrganisationUnit getAccessibleDescendant(
      User user, Program program, OrganisationUnit orgUnit, boolean includeAllDescendants) {

    Set<OrganisationUnit> userOrgUnits =
        isProgramAccessRestricted(program)
            ? user.getOrganisationUnits()
            : user.getTeiSearchOrganisationUnits();

    return includeAllDescendants
        ? findFirstDescendant(orgUnit, userOrgUnits)
        : findFirstChild(orgUnit, userOrgUnits);
  }

  /**
   * Finds the highest level org unit from the user scope whose path is contained in the requested
   * org unit path
   *
   * @param requestedOrgUnit org unit requested by the user
   * @param userOrgUnits org units defined in the user scope
   * @return if found, the org unit that matches the criteria, if not, null
   */
  private static OrganisationUnit findFirstDescendant(
      OrganisationUnit requestedOrgUnit, Set<OrganisationUnit> userOrgUnits) {
    for (OrganisationUnit orgUnit : userOrgUnits) {
      if (requestedOrgUnit.getPath().contains(orgUnit.getPath())) {
        return requestedOrgUnit;
      }
    }

    if (!requestedOrgUnit.hasChild()) {
      return null;
    }

    for (OrganisationUnit child : requestedOrgUnit.getChildren()) {
      OrganisationUnit descendant = findFirstDescendant(child, userOrgUnits);
      if (descendant != null) {
        return descendant;
      }
    }

    return null;
  }

  /**
   * Finds the highest level org unit from the user scope whose path is contained in the requested
   * org unit path. The org unit found can only be in the same level or one level below the
   * requested org unit.
   *
   * @param requestedOrgUnit org unit requested by the user
   * @param userOrgUnits org units defined in the user scope
   * @return if found, the org unit that matches the criteria, if not, null
   */
  private static OrganisationUnit findFirstChild(
      OrganisationUnit requestedOrgUnit, Set<OrganisationUnit> userOrgUnits) {
    for (OrganisationUnit orgUnit : userOrgUnits) {
      if (requestedOrgUnit.getPath().contains(orgUnit.getPath())) {
        return requestedOrgUnit;
      }
    }

    if (!requestedOrgUnit.hasChild()) {
      return null;
    }

    for (OrganisationUnit child : requestedOrgUnit.getChildren()) {
      for (OrganisationUnit orgUnit : userOrgUnits) {
        if (child.getPath().contains(orgUnit.getPath())) {
          return child;
        }
      }
    }

    return null;
  }

  private static boolean isProgramAccessRestricted(Program program) {
    return program != null && (program.isClosed() || program.isProtected());
  }

  private static Set<OrganisationUnit> getAccessibleOrgUnits(User user, Program program) {
    return isProgramAccessRestricted(program)
        ? new HashSet<>(user.getOrganisationUnits())
        : new HashSet<>(user.getTeiSearchOrganisationUnitsWithFallback());
  }

  private static OrganisationUnit getSelectedOrgUnit(
      User user,
      Program program,
      OrganisationUnit orgUnit,
      TrackerAccessManager trackerAccessManager) {
    return trackerAccessManager.canAccess(user, program, orgUnit) ? orgUnit : null;
  }
}
