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
package org.hisp.dhis.analytics.table;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.ColumnDataType.BOOLEAN;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.JSONB;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_1200;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NULL;
import static org.hisp.dhis.analytics.IndexType.GIN;
import static org.hisp.dhis.analytics.IndexType.GIST;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component( "org.hisp.dhis.analytics.TeiAnalyticsTableManager" )
public class JdbcTeiAnalyticsTableManager extends AbstractJdbcTableManager
{

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    public JdbcTeiAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, TrackedEntityTypeService trackedEntityTypeService,
        TrackedEntityAttributeService trackedEntityAttributeService )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService,
            tableHookService, statementBuilder, partitionManager, databaseInfo, jdbcTemplate );

        notNull( trackedEntityAttributeService, "trackedEntityAttributeService cannot be null" );
        this.trackedEntityAttributeService = trackedEntityAttributeService;

        notNull( trackedEntityTypeService, "trackedEntityTypeService cannot be null" );
        this.trackedEntityTypeService = trackedEntityTypeService;
    }

    private static final List<AnalyticsTableColumn> GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "trackedentityinstanceid" ), INTEGER, NOT_NULL,
            "tei.trackedentityinstanceid" ),
        new AnalyticsTableColumn( quote( "trackedentityinstanceuid" ), CHARACTER_11, NOT_NULL, "tei.uid" ),
        new AnalyticsTableColumn( quote( "trackedentitytypeuid" ), CHARACTER_11, NOT_NULL, "tet.uid" ),
        new AnalyticsTableColumn( quote( "created" ), TIMESTAMP, "tei.created" ),
        new AnalyticsTableColumn( quote( "lastupdated" ), TIMESTAMP, "tei.lastupdated" ),
        new AnalyticsTableColumn( quote( "inactive" ), BOOLEAN, "tei.inactive" ),
        new AnalyticsTableColumn( quote( "createdatclient" ), TIMESTAMP, "tei.createdatclient" ),
        new AnalyticsTableColumn( quote( "lastupdatedatclient" ), TIMESTAMP, "tei.lastupdatedatclient" ),
        new AnalyticsTableColumn( quote( "lastsynchronized" ), TIMESTAMP, "tei.lastsynchronized" ),
        new AnalyticsTableColumn( quote( "geometry" ), GEOMETRY, "tei.geometry" ).withIndexType( GIST ),
        new AnalyticsTableColumn( quote( "longitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(tei.geometry) THEN ST_X(tei.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "latitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(tei.geometry) THEN ST_Y(tei.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "featuretype" ), VARCHAR_255, NULL, "tei.featuretype" ),
        new AnalyticsTableColumn( quote( "coordinates" ), TEXT, NULL, "tei.coordinates" ),
        new AnalyticsTableColumn( quote( "storedby" ), VARCHAR_255, "tei.storedby" ),
        new AnalyticsTableColumn( quote( "potentialduplicate" ), BOOLEAN, NULL, "tei.potentialduplicate" ),
        new AnalyticsTableColumn( quote( "uidlevel1" ), CHARACTER_11, NULL, "ous.uidlevel1" ),
        new AnalyticsTableColumn( quote( "uidlevel2" ), CHARACTER_11, NULL, "ous.uidlevel2" ),
        new AnalyticsTableColumn( quote( "uidlevel3" ), CHARACTER_11, NULL, "ous.uidlevel3" ),
        new AnalyticsTableColumn( quote( "uidlevel4" ), CHARACTER_11, NULL, "ous.uidlevel4" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), VARCHAR_255, NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), CHARACTER_11, NULL, "ou.code" ),
        new AnalyticsTableColumn( quote( "oulevel" ), INTEGER, NULL, "ous.level" ) );

    private static final List<AnalyticsTableColumn> NON_GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "createdbyusername" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'username' as createdbyusername" ),
        new AnalyticsTableColumn( quote( "createdbyname" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'firstName' as createdbyname" ),
        new AnalyticsTableColumn( quote( "createdbylastname" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'surname' as createdbylastname" ),
        new AnalyticsTableColumn( quote( "createdbydisplayname" ), VARCHAR_255,
            getDisplayName( "createdbyuserinfo", "tei", "createdbydisplayname" ) ),
        new AnalyticsTableColumn( quote( "lastupdatedbyusername" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername" ),
        new AnalyticsTableColumn( quote( "lastupdatedbyname" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname" ),
        new AnalyticsTableColumn( quote( "lastupdatedbylastname" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname" ),
        new AnalyticsTableColumn( quote( "lastupdatedbydisplayname" ), VARCHAR_255,
            getDisplayName( "lastupdatedbyuserinfo", "tei", "lastupdatedbydisplayname" ) ) );

    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this
     * manager handles.
     *
     * @return type of analytics table.
     */
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return TRACKED_ENTITY_INSTANCE;
    }

    /**
     * Returns a {@link AnalyticsTable} with a list of yearly
     * {@link AnalyticsTablePartition}.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return the analytics table with partitions.
     */
    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        List<Program> programs = params.isSkipPrograms() ? idObjectManager.getAllNoAcl( Program.class )
            .stream()
            .filter( p -> !params.getSkipPrograms().contains( p.getUid() ) )
            .collect( toList() ) : idObjectManager.getAllNoAcl( Program.class );

        Map<String, List<Program>> programsByTetUid = programs.stream()
            .filter( program -> Objects.nonNull( program.getTrackedEntityType() ) )
            .collect( groupingBy(
                o -> o.getTrackedEntityType().getUid() ) );

        return trackedEntityTypeService.getAllTrackedEntityType()
            .stream()
            .map( tet -> new AnalyticsTable( getAnalyticsTableType(), getTableColumns( programsByTetUid, tet ),
                emptyList(), tet ) )
            .collect( toList() );
    }

    private List<AnalyticsTableColumn> getTableColumns( Map<String, List<Program>> programsByTetUid,
        TrackedEntityType tet )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>( getFixedColumns() );

        // Review this logic, it could result in many columns
        CollectionUtils.emptyIfNull( programsByTetUid.get( tet.getUid() ) )
            .forEach( program -> columns.add( new AnalyticsTableColumn( quote( program.getUid() ), BOOLEAN,
                " exists((select 1 from programinstance pi_0, program p_0"
                    + " where pi_0.trackedentityinstanceid = tei.trackedentityinstanceid"
                    + " and p_0.programid = pi_0.programid and p_0.uid='" + program.getUid() + "'))" ) ) );

        List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

        if ( programsByTetUid.containsKey( tet.getUid() ) )
        {
            trackedEntityAttributes = getAllTrackedEntityAttributes( tet,
                programsByTetUid.get( tet.getUid() ) );
        }

        columns.addAll( trackedEntityAttributes.stream()
            .map( BaseIdentifiableObject::getUid )
            .distinct()
            .map( teaUid -> new AnalyticsTableColumn( quote( teaUid ), VARCHAR_1200,
                " (select teavin.value from trackedentityattribute teain "
                    + " inner join trackedentityattributevalue teavin on teain.trackedentityattributeid = teavin.trackedentityattributeid "
                    + " where tei.trackedentityinstanceid = teavin.trackedentityinstanceid and teain.uid = '"
                    + teaUid + "')" ) )
            .collect( toList() ) );

        StringBuilder sql = new StringBuilder();

        columns.add(
            new AnalyticsTableColumn(
                quote( "enrollments" ), JSONB,
                sql.append( "(select JSON_AGG(JSON_BUILD_OBJECT('programUid', p_0.uid," )
                    .append( " 'programInstanceUid', pi_0.uid, 'enrollmentDate', pi_0.enrollmentdate," )
                    .append( " 'incidentDate', pi_0.incidentdate,'endDate', pi_0.enddate, 'events', " )
                    .append( " (select JSON_AGG(JSON_BUILD_OBJECT('programStageUid', ps_0.uid," )
                    .append( " 'programStageInstanceUid', psi_0.uid, 'executionDate', psi_0.executiondate," )
                    .append( " 'dueDate', psi_0.duedate, 'eventDataValues', psi_0.eventdatavalues))" )
                    .append( " from programstageinstance psi_0, programstage ps_0" )
                    .append( " where psi_0.programinstanceid = pi_0.programinstanceid" )
                    .append( " and ps_0.programstageid = psi_0.programstageid)))" )
                    .append( " from programinstance pi_0, program p_0" )
                    .append( " where pi_0.trackedentityinstanceid = tei.trackedentityinstanceid" )
                    .append( " and p_0.programid = pi_0.programid)" ).toString() ).withIndexType( GIN ) );
        return columns;
    }

    private List<TrackedEntityAttribute> getAllTrackedEntityAttributes( TrackedEntityType trackedEntityType,
        List<Program> programs )
    {
        List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

        trackedEntityAttributes.addAll( trackedEntityAttributeService.getProgramTrackedEntityAttributes( programs ) );

        trackedEntityAttributes.addAll(
            Optional.of( trackedEntityType )
                .map( TrackedEntityType::getTrackedEntityAttributes )
                .orElse( Collections.emptyList() ) );

        return trackedEntityAttributes;
    }

    /**
     * Checks if the database content is in valid state for analytics table
     * generation.
     *
     * @return null if valid, a descriptive string if invalid.
     */
    @Override
    public String validState()
    {
        return null;
    }

    /**
     * Returns a list of non-dynamic {@link AnalyticsTableColumn}.
     *
     * @return a List of {@link AnalyticsTableColumn}.
     */
    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        List<AnalyticsTableColumn> allFixedColumns = new ArrayList<>( GROUP_BY_COLS );
        allFixedColumns.addAll( NON_GROUP_BY_COLS );

        return allFixedColumns;
    }

    /**
     * Returns a list of table checks (constraints) for the given analytics
     * table partition.
     *
     * @param partition the {@link AnalyticsTablePartition}.
     */
    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return emptyList();
    }

    /**
     * Returns the partition column name for the analytics table type, or null
     * if the table type is not partitioned.
     */
    @Override
    protected String getPartitionColumn()
    {
        return null;
    }

    /**
     * Populates the given analytics table.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param partition the {@link AnalyticsTablePartition} to populate.
     */
    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );

        StringBuilder sql = new StringBuilder( "insert into " + partition.getTempTableName() + " (" );

        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            if ( col.isVirtual() )
            {
                continue;
            }

            sql.append( col.getName() + "," );
        }

        TextUtils.removeLastComma( sql ).append( ") select " );

        for ( AnalyticsTableColumn col : columns )
        {
            if ( col.isVirtual() )
            {
                continue;
            }

            sql.append( col.getAlias() + "," );
        }

        TextUtils.removeLastComma( sql )
            .append( " from trackedentityinstance tei" )
            .append( " left join trackedentitytype tet on tet.trackedentitytypeid = tei.trackedentitytypeid" )
            .append( " left join programinstance pi on pi.trackedentityinstanceid = tei.trackedentityinstanceid" )
            .append( " and tei.deleted is false" )
            .append( " left join program p on p.programid = pi.programid and pi.deleted is false" )
            .append( " left join programstageinstance psi on psi.programinstanceid = pi.programinstanceid" )
            .append( " left join programstage ps on ps.programstageid = psi.programstageid" )
            .append( " left join organisationunit ou on tei.organisationunitid = ou.organisationunitid" )
            .append( " left join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid" )
            .append( " where tei.trackedentitytypeid = " + partition.getMasterTable().getTrackedEntityType().getId() )
            .append( " and psi.status in (" + String.join( ",", EXPORTABLE_EVENT_STATUSES ) + ")" +
                "and psi.deleted is false " )
            .append( " group by " )
            .append( GROUP_BY_COLS.stream()
                .map( AnalyticsTableColumn::getAlias )
                .filter( alias -> !"enrollments".equals( alias ) )
                .collect( joining( "," ) ) );

        invokeTimeAndLog( sql.toString(), partition.getTempTableName() );
    }

    /**
     * Indicates whether data was created or updated for the given time range
     * since last successful "latest" table partition update.
     *
     * @param startDate the start date.
     * @param endDate the end date.
     * @return true if updated data exists.
     */
    @Override
    protected boolean hasUpdatedLatestData( Date startDate, Date endDate )
    {
        return false;
    }
}
