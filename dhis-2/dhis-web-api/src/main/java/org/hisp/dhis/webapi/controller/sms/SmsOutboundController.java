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
package org.hisp.dhis.webapi.controller.sms;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_MOBILE_SENDSMS;
import static org.hisp.dhis.security.Authorities.F_MOBILE_SETTINGS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zubair Asghar
 */
@RestController
@RequestMapping("/api/sms/outbound")
@OpenApi.Document(classifiers = {"team:tracker", "purpose:data"})
public class SmsOutboundController
    extends AbstractCrudController<OutboundSms, GetObjectListParams> {
  private final MessageSender smsMessageSender;

  private final RenderService renderService;

  private final OutboundSmsService outboundSmsService;

  public SmsOutboundController(
      MessageSender smsMessageSender,
      RenderService renderService,
      OutboundSmsService outboundSmsService) {
    this.smsMessageSender = smsMessageSender;
    this.renderService = renderService;
    this.outboundSmsService = outboundSmsService;
  }

  @Override
  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping
  public @ResponseBody ResponseEntity<StreamingJsonRoot<OutboundSms>> getObjectList(
      GetObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, ConflictException {
    return super.getObjectList(params, response, currentUser);
  }

  // -------------------------------------------------------------------------
  // POST
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PostMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendSMSMessage(@RequestParam String recipient, @RequestParam String message) {
    if (recipient == null || recipient.length() <= 0) {
      return conflict("Recipient must be specified");
    }

    if (message == null || message.length() <= 0) {
      return conflict("Message must be specified");
    }

    OutboundMessageResponse status = smsMessageSender.sendMessage(null, message, recipient);

    if (status.isOk()) {
      return ok("SMS sent");
    }
    return error(status.getDescription());
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendSMSMessage(HttpServletRequest request) throws IOException {
    OutboundSms sms = renderService.fromJson(request.getInputStream(), OutboundSms.class);

    OutboundMessageResponse status =
        smsMessageSender.sendMessage(null, sms.getMessage(), sms.getRecipients());

    if (status.isOk()) {
      return ok("SMS sent");
    }
    return error(status.getDescription());
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  @DeleteMapping(value = "/{uid}", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_MOBILE_SETTINGS)
  @ResponseBody
  public WebMessage deleteOutboundMessage(@PathVariable String uid) {
    OutboundSms sms = outboundSmsService.get(uid);

    if (sms == null) {
      return notFound("No OutboundSms with id '" + uid + "' was found.");
    }

    outboundSmsService.delete(uid);

    return ok("OutboundSms with " + uid + " deleted");
  }

  @DeleteMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_MOBILE_SETTINGS)
  @ResponseBody
  public WebMessage deleteOutboundMessages(@RequestParam List<String> ids) {
    ids.forEach(outboundSmsService::delete);

    return ok("Objects deleted");
  }
}
