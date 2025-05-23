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
package org.hisp.dhis.tracker.imports.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.tracker.TrackerType;

/**
 * This immutable object collects all the relevant information created during a Tracker Import
 * process.
 *
 * <p>The TrackerImportReport is created at the beginning of a Tracker Import session and returned
 * to the caller.
 *
 * @author Luciano Fiandesio
 */
@OpenApi.Shared(name = "TrackerImportReport")
@Getter
@ToString
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImportReport {

  /**
   * The global status of the Import operation.
   *
   * <p>- OK - no errors ( including validation errors )
   *
   * <p>- WARNING - at least one Warning was collected during the Import
   *
   * <p>- ERROR - at least on Error was collected during the Import
   */
  @JsonProperty Status status;

  /** A list of all validation errors occurred during the validation process */
  @JsonProperty ValidationReport validationReport;

  /**
   * A final summary broken down by operation (Insert, Update, Delete, Ignored) showing how many
   * entities where processed
   */
  @JsonProperty Stats stats;

  /** A report containing the outcome of the commit stage (e.g. how many entities were persisted) */
  @JsonProperty("bundleReport")
  PersistenceReport persistenceReport;

  /**
   * A message to attach to the report. This message is designed to be used only if a catastrophic
   * error occurs and the Import Process has to stop abruptly.
   */
  @JsonProperty String message;

  /**
   * Factory method to use in case one or more Validation errors are present in the {@link
   * ValidationReport} and the Import process needs to exit without attempting persistence.
   *
   * <p>Import statistics are calculated assuming that the persistence stage was never attempted,
   * therefore all bundle objects were ignored.
   *
   * @param validationReport The validation report
   * @param bundleSize The sum of all bundle objects
   */
  public static ImportReport withValidationErrors(
      ValidationReport validationReport, int bundleSize) {
    return builder()
        .status(Status.ERROR)
        .validationReport(validationReport)
        .stats(Stats.builder().ignored(bundleSize).build())
        .build();
  }

  /**
   * Factory method to use in case of an unrecoverable error during the Tracker Import. This factory
   * method will set the status to ERROR.
   *
   * <p>This method should only be used when the Import process has to exit.
   *
   * <p>Import statistics are not calculated.
   *
   * @param message The error message
   * @param validationReport The validation report if available
   */
  public static ImportReport withError(String message, ValidationReport validationReport) {
    // TODO shall we calculate stats in this case?
    return builder()
        .status(Status.ERROR)
        .validationReport(validationReport)
        .message(message)
        .build();
  }

  /**
   * Factory method to use when a Tracker Import process completes.
   *
   * <p>Import statistics are calculated based on the {@link PersistenceReport} and {@link
   * ValidationReport}.
   *
   * @param status The outcome of the process
   * @param persistenceReport The report containing how many bundle objects were successfully
   *     persisted
   * @param validationReport The validation report if available
   * @param bundleSize a map containing the size of each entity type in the Bundle - before the
   *     validation
   */
  public static ImportReport withImportCompleted(
      Status status,
      PersistenceReport persistenceReport,
      ValidationReport validationReport,
      Map<TrackerType, Integer> bundleSize) {
    Stats stats = Stats.builder().build();
    Stats brs = persistenceReport.getStats();
    stats.merge(brs);
    stats.setIgnored(Math.toIntExact(validationReport.size()) + brs.getIgnored());
    return builder()
        .status(status)
        .validationReport(validationReport)
        .persistenceReport(processBundleReport(persistenceReport, bundleSize))
        .stats(stats)
        .build();
  }

  /**
   * Calculates the 'ignored' value for each type of entity in the {@link PersistenceReport}.
   *
   * <p>The 'ignored' value is calculated by subtracting the sum of all processed entities from the
   * TrackerBundleReport (by type) from the bundle size specified in the 'bundleSize' map.
   */
  private static PersistenceReport processBundleReport(
      PersistenceReport persistenceReport, Map<TrackerType, Integer> bundleSize) {
    for (final TrackerType value : TrackerType.values()) {
      final TrackerTypeReport trackerTypeReport = persistenceReport.getTypeReportMap().get(value);
      if (trackerTypeReport != null) {
        final Stats stats = trackerTypeReport.getStats();
        if (stats != null) {
          int statsSize = stats.getDeleted() + stats.getCreated() + stats.getUpdated();
          stats.setIgnored(bundleSize.getOrDefault(value, statsSize) - statsSize);
        }
      }
    }
    return persistenceReport;
  }
}
