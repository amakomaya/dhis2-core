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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserRoleTest extends PostgresIntegrationTestBase {

  @Autowired
  @Qualifier("org.hisp.dhis.user.UserRoleStore")
  private IdentifiableObjectStore<UserRole> userRoleStore;

  @Test
  void testAddGetUserRole() {
    UserRole roleA = createUserRole('A');
    UserRole roleB = createUserRole('B');
    UserRole roleC = createUserRole('C');
    userRoleStore.save(roleA);
    long idA = roleA.getId();
    userRoleStore.save(roleB);
    long idB = roleB.getId();
    userRoleStore.save(roleC);
    long idC = roleC.getId();
    assertEquals(roleA, userRoleStore.get(idA));
    assertEquals(roleB, userRoleStore.get(idB));
    assertEquals(roleC, userRoleStore.get(idC));
  }

  @Test
  void testDeleteUserRole() {
    UserRole roleA = createUserRole('A');
    UserRole roleB = createUserRole('B');
    UserRole roleC = createUserRole('C');
    userRoleStore.save(roleA);
    long idA = roleA.getId();
    userRoleStore.save(roleB);
    long idB = roleB.getId();
    userRoleStore.save(roleC);
    long idC = roleC.getId();
    assertEquals(roleA, userRoleStore.get(idA));
    assertEquals(roleB, userRoleStore.get(idB));
    assertEquals(roleC, userRoleStore.get(idC));
    userRoleStore.delete(roleB);
    assertNotNull(userRoleStore.get(idA));
    assertNull(userRoleStore.get(idB));
    assertNotNull(userRoleStore.get(idA));
  }

  @Test
  void testOidcUserIsAuthorizedCheck() {
    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "test-sub");

    DhisOidcUser dhisOidcUser =
        new DhisOidcUser(currentUserDetails, attributes, IdTokenClaimNames.SUB, null);
    injectSecurityContext(dhisOidcUser);

    UserDetails oidc = CurrentUserUtil.getCurrentUserDetails();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication.getPrincipal();
    assertEquals(dhisOidcUser, principal);

    Collection<? extends GrantedAuthority> authorities = oidc.getAuthorities();

    for (GrantedAuthority authority : authorities) {
      assertTrue(oidc.isAuthorized(authority.getAuthority()));
    }

    for (GrantedAuthority authority : authorities) {
      assertTrue(oidc.hasAnyAuthority(Set.of(authority.getAuthority())));
    }
  }
}
