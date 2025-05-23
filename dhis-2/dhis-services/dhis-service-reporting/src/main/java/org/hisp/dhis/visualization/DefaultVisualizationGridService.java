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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DisplayProperty.SHORTNAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DefaultVisualizationGridService implements VisualizationGridService {
  private final VisualizationService visualizationService;

  private final AnalyticsService analyticsService;

  private final OrganisationUnitService organisationUnitService;

  private final I18nManager i18nManager;

  private final UserService userService;

  @Override
  @Transactional(readOnly = true)
  public Grid getVisualizationGrid(String uid, Date relativePeriodDate, String orgUnitUid) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    return getVisualizationGrid(uid, relativePeriodDate, orgUnitUid, currentUser);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getVisualizationGrid(
      String uid, Date relativePeriodDate, String organisationUnitUid, User user) {
    Visualization visualization = visualizationService.getVisualization(uid);

    Grid grid = null;

    if (visualization != null) {
      I18nFormat format = i18nManager.getI18nFormat();
      OrganisationUnit organisationUnit =
          organisationUnitService.getOrganisationUnit(organisationUnitUid);

      List<OrganisationUnit> atLevels = new ArrayList<>();
      List<OrganisationUnit> inGroups = new ArrayList<>();

      if (visualization.hasOrganisationUnitLevels()) {
        atLevels.addAll(
            organisationUnitService.getOrganisationUnitsAtLevels(
                visualization.getOrganisationUnitLevels(), visualization.getOrganisationUnits()));
      }

      if (visualization.hasItemOrganisationUnitGroups()) {
        inGroups.addAll(
            organisationUnitService.getOrganisationUnits(
                visualization.getItemOrganisationUnitGroups(),
                visualization.getOrganisationUnits()));
      }

      visualization.init(user, relativePeriodDate, organisationUnit, atLevels, inGroups, format);

      Map<String, Object> valueMap = analyticsService.getAggregatedDataValueMapping(visualization);

      grid = visualization.getGrid(new ListGrid(), valueMap, SHORTNAME, true);

      visualization.clearTransientState();
    }

    return ObjectUtils.firstNonNull(grid, new ListGrid());
  }
}
