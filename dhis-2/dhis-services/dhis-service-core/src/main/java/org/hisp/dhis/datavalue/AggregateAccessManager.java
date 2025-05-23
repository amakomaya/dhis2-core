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
package org.hisp.dhis.datavalue;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public interface AggregateAccessManager {
  /**
   * Check if given User has DATA_READ access for given DataValue
   *
   * @param userDetails to check permission for
   * @param dataValue a {@link DataValue} object
   * @return List of errors
   */
  List<String> canRead(UserDetails userDetails, DataValue dataValue);

  /**
   * Check if given user has DATA_WRITE
   *
   * @param userDetails to check permission for
   * @param dataSet a {@link DataSet} object
   * @return List of errors
   */
  List<String> canWrite(UserDetails userDetails, DataSet dataSet);

  /**
   * Check if given User has DATA_READ access for given DataSet
   *
   * @param userDetails to check permission for
   * @param dataSet a {@link DataValue} object
   * @return List of errors
   */
  List<String> canRead(UserDetails userDetails, DataSet dataSet);

  /**
   * Check if given User has DATA_WRITE access for given CategoryOptionCombo
   *
   * @param userDetails to check permission for
   * @param categoryOption a {@link CategoryOptionCombo} object
   * @return List of errors
   */
  List<String> canWrite(UserDetails userDetails, CategoryOptionCombo categoryOption);

  /**
   * Check if given User has DATA_WRITE access for given CategoryOptionCombo, result is cached.
   *
   * @param userDetails to check permission for
   * @param categoryOptionCombo a {@link CategoryOptionCombo} object
   * @return List of errors
   */
  List<String> canWriteCached(UserDetails userDetails, CategoryOptionCombo categoryOptionCombo);

  /**
   * Check if given User has DATA_READ access for given CategoryOptionCombo
   *
   * @param userDetails to check permission for
   * @param categoryOption a {@link CategoryOptionCombo} object
   * @return List of errors
   */
  List<String> canRead(UserDetails userDetails, CategoryOptionCombo categoryOption);

  /**
   * Check if given User has DATA_WRITE access for give DataElementOperand
   *
   * @param userDetails to check permission for
   * @param dataElementOperand a {@link DataElementOperand} object
   * @return List of errors
   */
  List<String> canWrite(UserDetails userDetails, DataElementOperand dataElementOperand);
}
