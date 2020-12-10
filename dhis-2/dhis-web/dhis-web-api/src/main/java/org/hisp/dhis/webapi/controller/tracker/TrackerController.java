package org.hisp.dhis.webapi.controller.tracker;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.job.TrackerJobWebMessageResponse;
import org.hisp.dhis.tracker.job.TrackerMessageManager;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import lombok.RequiredArgsConstructor;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping( value = TrackerController.RESOURCE_PATH )
public class TrackerController
{
    public static final String RESOURCE_PATH = "/tracker";

    private final TrackerImportService trackerImportService;

    private final RenderService renderService;

    private final ContextService contextService;

    private final TrackerMessageManager trackerMessageManager;

    private final Notifier notifier;

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    // @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKER_IMPORTER_EXPERIMENTAL')"
    // )
    public void postJsonTracker( HttpServletRequest request, HttpServletResponse response, User currentUser )
        throws IOException
    {
        // Set the Import Parameters
        TrackerImportParams params = TrackerImportParamsBuilder.build( contextService.getParameterValuesMap() );

        // Set the actual objects to import
        TrackerBundleParams trackerBundleParams = renderService.fromJson( request.getInputStream(),
            TrackerBundleParams.class );
        params.setUserId( currentUser.getUid() );
        params.setTrackedEntities( trackerBundleParams.getTrackedEntities() );
        params.setEnrollments( trackerBundleParams.getEnrollments() );
        params.setEvents( trackerBundleParams.getEvents() );
        params.setRelationships( trackerBundleParams.getRelationships() );

        runAsyncJob( params, request, response );
    }

    @GetMapping( value = "/jobs/{uid}", produces = MediaType.APPLICATION_JSON_VALUE )
    public List<Notification> getJob( @PathVariable String uid, HttpServletResponse response )
        throws HttpStatusCodeException
    {
        List<Notification> notifications = notifier.getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        setNoStore( response );

        return notifications;
    }

    @GetMapping( value = "/jobs/{uid}/report", produces = MediaType.APPLICATION_JSON_VALUE )
    public TrackerImportReport getJobReport( @PathVariable String uid,
        @RequestParam( defaultValue = "errors", required = false ) String reportMode,
        HttpServletResponse response )
        throws HttpStatusCodeException
    {
        TrackerBundleReportMode trackerBundleReportMode;
        try
        {
            trackerBundleReportMode = TrackerBundleReportMode.valueOf( reportMode.toUpperCase() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new HttpClientErrorException( HttpStatus.BAD_REQUEST,
                "Value " + reportMode + " is not a valid report mode" );
        }

        Object importReport = notifier.getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        setNoStore( response );

        if ( importReport != null )
        {
            return trackerImportService.buildImportReport( (TrackerImportReport) importReport,
                trackerBundleReportMode );
        }

        throw new HttpClientErrorException( HttpStatus.NOT_FOUND );
    }

    @PatchMapping( value = "/events/{uid}" )
    public void patchEvents( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response,
        User currentUser )
        throws IOException
    {
        Event entity = renderService.fromJson( request.getInputStream(), Event.class );
        entity.setEvent( uid );

        patch( request, response, currentUser, null, null, entity );
    }

    @PatchMapping( value = "/enrollments/{uid}" )
    public void patchEnrollments( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response,
        User currentUser )
        throws IOException
    {
        Enrollment entity = renderService.fromJson( request.getInputStream(), Enrollment.class );
        entity.setEnrollment( uid );

        patch( request, response, currentUser, null, entity, null );
    }

    @PatchMapping( value = "/trackedEntities/{uid}" )
    public void patchTrackedEntity( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response,
        User currentUser )
        throws IOException
    {
        TrackedEntity entity = renderService.fromJson( request.getInputStream(), TrackedEntity.class );
        entity.setTrackedEntity( uid );

        patch( request, response, currentUser, entity, null, null );
    }

    private void patch( HttpServletRequest request, HttpServletResponse response, User currentUser,
        TrackedEntity trackedEntity, Enrollment enrollment, Event event )
        throws IOException
    {

        TrackerImportParams params = TrackerImportParams.builder()
            .validationMode( ValidationMode.FULL )
            .importStrategy( TrackerImportStrategy.PATCH )
            .skipRuleEngine( true )
            .userId( currentUser.getUid() )
            .events( event == null ? new ArrayList<>() : Collections.singletonList( event ) )
            .enrollments( enrollment == null ? new ArrayList<>() : Collections.singletonList( enrollment ) )
            .trackedEntities( trackedEntity == null ? new ArrayList<>() : Collections.singletonList( trackedEntity ) )
            .build();

        runAsyncJob( params, request, response );
    }

    private void runAsyncJob( TrackerImportParams params, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        String jobId = trackerMessageManager.addJob( params );

        String location = ContextUtils.getRootPath( request ) + "/tracker/jobs/" + jobId;
        response.setHeader( "Location", location );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), new WebMessage()
            .setMessage( "Tracker job added" )
            .setResponse(
                TrackerJobWebMessageResponse.builder()
                    .id( jobId ).location( location )
                    .build() ) );
    }
}
