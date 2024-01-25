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
package org.hisp.dhis.useraccount.action;

import com.opensymphony.xwork2.Action;
import javax.servlet.http.HttpSession;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.RestoreType;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
public class IsInviteTokenValidAction implements Action {
  @Autowired private SecurityService securityService;

  @Autowired private UserService userService;

  // -------------------------------------------------------------------------
  // Input
  // -------------------------------------------------------------------------

  private String token;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  public String getAccountAction() {
    return "invited";
  }

  private String usernameChoice;

  public String getUsernameChoice() {
    return usernameChoice;
  }

  private String email;

  public String getEmail() {
    return email;
  }

  private String username;

  public String getUsername() {
    return username;
  }

  private String firstName;
  private String surname;
  private String phoneNumber;

  public String getFirstName() {
    return firstName;
  }

  public String getSurname() {
    return surname;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  private String cspNonce = "";

  public void setCspNonce(String cspNonce) {
    this.cspNonce = cspNonce;
  }

  public String getCspNonce() {
    return cspNonce;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() {
    String[] idAndRestoreToken = securityService.decodeEncodedTokens(token);
    String idToken = idAndRestoreToken[0];
    String restoreToken = idAndRestoreToken[1];

    User user = userService.getUserByIdToken(idToken);

    if (user == null) {
      return ERROR;
    }

    String errorMessage =
        securityService.verifyRestoreToken(user, restoreToken, RestoreType.INVITE);

    if (errorMessage != null) {
      return ERROR;
    }

    HttpSession session = ServletActionContext.getRequest().getSession();
    String nonce = (String) session.getAttribute("nonce");
    setCspNonce(nonce);

    email = user.getEmail();
    username = user.getUsername();
    firstName = user.getFirstName();
    surname = user.getSurname();
    phoneNumber = user.getPhoneNumber();

    RestoreOptions restoreOptions = securityService.getRestoreOptions(restoreToken);

    if (restoreOptions != null) {
      usernameChoice = Boolean.toString(restoreOptions.isUsernameChoice());
    }

    return SUCCESS;
  }
}
