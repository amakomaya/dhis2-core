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
package org.hisp.dhis.tracker.acl;

import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;

/**
 * @author Ameen Mohamed
 */
public interface TrackedEntityProgramOwnerService {
  String ID = TrackedEntityProgramOwnerService.class.getName();

  /**
   * Get the program owner details for a tracked entity.
   *
   * @return The TrackedEntityProgramOwner object
   */
  TrackedEntityProgramOwner getTrackedEntityProgramOwner(TrackedEntity te, Program program);

  /**
   * Assign an orgUnit as the owner for a tracked entity for the given program. If another owner
   * already exist then it would be overwritten.
   */
  void createOrUpdateTrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit);

  /**
   * Update the owner ou for a tracked entity for the given program. If no owner previously exist,
   * then this method will fail.
   */
  void updateTrackedEntityProgramOwner(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull Program program,
      @Nonnull OrganisationUnit orgUnit)
      throws BadRequestException;

  /**
   * Create a new program owner ou for a tracked entity. If an owner previously exist, then this
   * method will fail.
   */
  void createTrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit);
}
