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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Abyot Asalefew
 */
public interface EnrollmentService {

  /**
   * Soft deletes a {@link Enrollment}.
   *
   * @param enrollment the Enrollment to delete.
   */
  void deleteEnrollment(Enrollment enrollment);

  /**
   * Hard deletes a {@link Enrollment}.
   *
   * @param enrollment the Enrollment to delete.
   */
  void hardDeleteEnrollment(Enrollment enrollment);

  /** Get enrollments into a program. */
  List<Enrollment> getEnrollments(Program program);

  /** Get enrollments into a program that are in given status. */
  List<Enrollment> getEnrollments(Program program, EnrollmentStatus status);

  /** Get a tracked entities enrollments into a program that are in given status. */
  List<Enrollment> getEnrollments(
      TrackedEntity trackedEntity, Program program, EnrollmentStatus status);

  /**
   * Enroll a TrackedEntity into a program. Must be run inside a transaction.
   *
   * @param trackedEntity TrackedEntity
   * @param program Program
   * @param enrollmentDate The date of enrollment
   * @param incidentDate The date of incident
   * @param orgunit Organisation Unit
   * @param uid UID to use for new instance
   * @return Enrollment
   */
  Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date incidentDate,
      OrganisationUnit orgunit,
      String uid);

  /**
   * Enroll a tracked entity into a program. Must be run inside a transaction.
   *
   * @param trackedEntity TrackedEntity
   * @param program Program
   * @param enrollmentDate The date of enrollment
   * @param incidentDate The date of incident
   * @param orgunit Organisation Unit
   * @return Enrollment
   */
  Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      Date enrollmentDate,
      Date incidentDate,
      OrganisationUnit orgunit);
}
