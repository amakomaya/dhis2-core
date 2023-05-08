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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper.toOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.applyIfNonEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseUids;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps query parameters from {@link EnrollmentsExportController} stored in
 * {@link EnrollmentCriteria} to {@link EnrollmentQueryParams} which is used to
 * fetch enrollments from the DB.
 */
@Service( "org.hisp.dhis.webapi.controller.tracker.export.EnrollmentCriteriaMapper" )
@RequiredArgsConstructor
public class EnrollmentCriteriaMapper
{

    @Nonnull
    private final CurrentUserService currentUserService;

    @Nonnull
    private final OrganisationUnitService organisationUnitService;

    @Nonnull
    private final ProgramService programService;

    @Nonnull
    private final TrackedEntityTypeService trackedEntityTypeService;

    @Nonnull
    private final TrackedEntityInstanceService trackedEntityInstanceService;

    @Nonnull
    private final TrackerAccessManager trackerAccessManager;

    @Transactional( readOnly = true )
    public EnrollmentQueryParams map( EnrollmentCriteria criteria )
        throws BadRequestException,
        ForbiddenException
    {
        Program program = applyIfNonEmpty( programService::getProgram, criteria.getProgram() );
        validateProgram( criteria.getProgram(), program );

        TrackedEntityType trackedEntityType = applyIfNonEmpty( trackedEntityTypeService::getTrackedEntityType,
            criteria.getTrackedEntityType() );
        validateTrackedEntityType( criteria.getTrackedEntityType(), trackedEntityType );

        TrackedEntityInstance trackedEntity = applyIfNonEmpty( trackedEntityInstanceService::getTrackedEntityInstance,
            criteria.getTrackedEntity() );
        validateTrackedEntityInstance( criteria.getTrackedEntity(), trackedEntity );

        User user = currentUserService.getCurrentUser();
        Set<String> orgUnitIds = parseUids( criteria.getOrgUnit() );
        Set<OrganisationUnit> orgUnits = validateOrgUnits( user, orgUnitIds, program );

        EnrollmentQueryParams params = new EnrollmentQueryParams();
        params.setProgram( program );
        params.setProgramStatus( criteria.getProgramStatus() );
        params.setFollowUp( criteria.getFollowUp() );
        params.setLastUpdated( criteria.getUpdatedAfter() );
        params.setLastUpdatedDuration( criteria.getUpdatedWithin() );
        params.setProgramStartDate( criteria.getEnrolledAfter() );
        params.setProgramEndDate( criteria.getEnrolledBefore() );
        params.setTrackedEntityType( trackedEntityType );
        params.setTrackedEntityInstanceUid(
            Optional.ofNullable( trackedEntity ).map( IdentifiableObject::getUid ).orElse( null ) );
        params.addOrganisationUnits( orgUnits );
        params.setOrganisationUnitMode( criteria.getOuMode() );
        params.setPage( criteria.getPage() );
        params.setPageSize( criteria.getPageSize() );
        params.setTotalPages( criteria.isTotalPages() );
        params.setSkipPaging( toBooleanDefaultIfNull( criteria.isSkipPaging(), false ) );
        params.setIncludeDeleted( criteria.isIncludeDeleted() );
        params.setUser( user );
        params.setOrder( toOrderParams( criteria.getOrder() ) );

        return params;
    }

    private static void validateProgram( String id, Program program )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && program == null )
        {
            throw new BadRequestException( "Program is specified but does not exist: " + id );
        }
    }

    private void validateTrackedEntityType( String id, TrackedEntityType trackedEntityType )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && trackedEntityType == null )
        {
            throw new BadRequestException( "Tracked entity type is specified but does not exist: " + id );
        }
    }

    private void validateTrackedEntityInstance( String id, TrackedEntityInstance trackedEntityInstance )
        throws BadRequestException
    {
        if ( isNotEmpty( id ) && trackedEntityInstance == null )
        {
            throw new BadRequestException( "Tracked entity instance is specified but does not exist: " + id );
        }
    }

    private Set<OrganisationUnit> validateOrgUnits( User user, Set<String> orgUnitIds, Program program )
        throws BadRequestException,
        ForbiddenException
    {
        Set<OrganisationUnit> orgUnits = new HashSet<>();
        if ( orgUnitIds != null )
        {
            for ( String orgUnitId : orgUnitIds )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnitId );

                if ( organisationUnit == null )
                {
                    throw new BadRequestException( "Organisation unit does not exist: " + orgUnitId );
                }

                if ( !trackerAccessManager.canAccess( user, program, organisationUnit ) )
                {
                    throw new ForbiddenException(
                        "User does not have access to organisation unit: " + organisationUnit.getUid() );
                }
                orgUnits.add( organisationUnit );
            }
        }

        return orgUnits;
    }
}