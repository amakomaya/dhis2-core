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
package org.hisp.dhis.analytics.table.model;

import static org.hisp.dhis.db.model.Table.fromStaging;
import static org.hisp.dhis.db.model.Table.toStaging;

import java.util.Date;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Class representing an analytics database table partition.
 *
 * @author Lars Helge Overland
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnalyticsTablePartition {
  public static final Integer LATEST_PARTITION = 0;

  /** Table name. */
  @EqualsAndHashCode.Include private String name;

  /** The master analytics table for this partition. */
  private AnalyticsTable masterTable;

  /**
   * The year for which this partition may contain data, where 0 indicates the "latest" data stored
   * since last full analytics table generation.
   */
  private Integer year;

  /** The start date for which this partition may contain data, inclusive. */
  private Date startDate;

  /** The end date for which this partition may contain data, exclusive. */
  private Date endDate;

  /**
   * Constructor. Sets the name to represent a staging table partition.
   *
   * @param masterTable the master {@link Table} of this partition.
   * @param year the year which represents this partition.
   * @param startDate the start date of data for this partition.
   * @param endDate the end date of data for this partition.
   */
  public AnalyticsTablePartition(
      AnalyticsTable masterTable, Integer year, Date startDate, Date endDate) {
    this.name = toStaging(getTableName(masterTable.getMainName(), year));
    this.masterTable = masterTable;
    this.year = year;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  // -------------------------------------------------------------------------
  // Static methods
  // -------------------------------------------------------------------------

  /**
   * Returns a table partition name.
   *
   * @param baseName the base name.
   * @param year the year.
   * @return a table partition name.
   */
  private static String getTableName(String baseName, Integer year) {
    String name = baseName;

    if (year != null) {
      name += "_" + year;
    }

    return name;
  }

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  /**
   * Returns the main table partition name.
   *
   * @return the main table partition name.
   */
  public String getMainName() {
    return fromStaging(name);
  }

  public boolean isLatestPartition() {
    return Objects.equals(year, LATEST_PARTITION);
  }

  @Override
  public String toString() {
    return name;
  }
}
