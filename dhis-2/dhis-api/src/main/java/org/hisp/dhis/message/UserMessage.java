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
package org.hisp.dhis.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.UUID;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "userMessage", namespace = DxfNamespaces.DXF_2_0)
public class UserMessage {
  private int id;

  private String key;

  private User user;

  private boolean read;

  private boolean followUp;

  private transient String lastRecipientSurname;

  private transient String lastRecipientFirstname;

  public String getLastRecipientSurname() {
    return lastRecipientSurname;
  }

  public void setLastRecipientSurname(String lastRecipientSurname) {
    this.lastRecipientSurname = lastRecipientSurname;
  }

  public String getLastRecipientFirstname() {
    return lastRecipientFirstname;
  }

  public void setLastRecipientFirstname(String lastRecipientFirstname) {
    this.lastRecipientFirstname = lastRecipientFirstname;
  }

  public String getLastRecipientName() {
    return lastRecipientFirstname + " " + lastRecipientSurname;
  }

  public UserMessage() {
    this.key = UUID.randomUUID().toString();
  }

  public UserMessage(User user) {
    this.key = UUID.randomUUID().toString();
    this.user = user;
    this.read = false;
  }

  public UserMessage(User user, boolean read) {
    this.key = UUID.randomUUID().toString();
    this.user = user;
    this.read = read;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isFollowUp() {
    return followUp;
  }

  public void setFollowUp(boolean followUp) {
    this.followUp = followUp;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (getClass() != object.getClass()) {
      return false;
    }

    final UserMessage other = (UserMessage) object;

    return key.equals(other.key);
  }

  @Override
  public String toString() {
    return "[User: " + user + ", read: " + read + "]";
  }
}
