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
package org.hisp.dhis.email;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;

/** Created by zubair on 30.03.17. */
public class EmailConfiguration {
  private String hostName;

  private String username;

  private String password;

  private String from;

  private int port;

  private boolean tls;

  public EmailConfiguration(
      String hostName, String username, String password, String from, int port, boolean tls) {
    this.hostName = StringUtils.trimToNull(hostName);
    this.username = StringUtils.trimToNull(username);
    this.password = StringUtils.trimToNull(password);
    this.from = from;
    this.port = port;
    this.tls = tls;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("Host name", hostName)
        .add("Username", username)
        .add("From", from)
        .add("Port", port)
        .add("TLS", tls)
        .toString();
  }

  public boolean isOk() {
    return hostName != null && username != null && password != null;
  }

  public String getHostName() {
    return hostName;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getFrom() {
    return from;
  }

  public int getPort() {
    return port;
  }

  public boolean isTls() {
    return tls;
  }
}
