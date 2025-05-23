/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.trackedentity.query.EnrolledInProgramCondition;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for adding the "is enrolled in program" filter to the sql query. The
 * generated conditions are "ungrouped", since each one needs to be a separate "AND" condition.
 */
@Service
public class EnrolledInProgramQueryBuilder extends SqlQueryBuilderAdaptor {

  private static final String PROGRAM_ENROLLED_GROUP = "PROGRAM_ENROLLED_GROUP";

  @Override
  protected Stream<GroupableCondition> getWhereClauses(
      QueryContext queryContext, List<DimensionIdentifier<DimensionParam>> unused) {

    boolean hasProgramsFromRequest =
        queryContext.getContextParams().getCommonRaw().getInternal().isRequestPrograms();

    Function<String, GroupableCondition> conditionMapper =
        EnrolledInProgramQueryBuilder::asGroupedEnrolledInProgramCondition;

    if (hasProgramsFromRequest) {
      conditionMapper = EnrolledInProgramQueryBuilder::asUngroupedEnrolledInProgramCondition;
    }

    return queryContext.getContextParams().getCommonParsed().getPrograms().stream()
        .map(IdentifiableObject::getUid)
        .map(conditionMapper);
  }

  private static GroupableCondition asGroupedEnrolledInProgramCondition(String programUid) {
    return GroupableCondition.of(PROGRAM_ENROLLED_GROUP, EnrolledInProgramCondition.of(programUid));
  }

  private static GroupableCondition asUngroupedEnrolledInProgramCondition(String programUid) {
    return GroupableCondition.ofUngroupedCondition(EnrolledInProgramCondition.of(programUid));
  }

  @Override
  public boolean alwaysRun() {
    return true;
  }
}
