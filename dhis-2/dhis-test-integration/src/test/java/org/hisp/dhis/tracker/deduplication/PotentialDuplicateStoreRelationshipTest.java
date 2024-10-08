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
package org.hisp.dhis.tracker.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Disabled(
    "moveRelationships method do not really belong to a store now. We should a better place for it")
class PotentialDuplicateStoreRelationshipTest extends PostgresIntegrationTestBase {

  @Autowired private HibernatePotentialDuplicateStore potentialDuplicateStore;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private DbmsManager dbmsManager;

  private TrackedEntity original;

  private TrackedEntity duplicate;

  private TrackedEntity extra1;

  private TrackedEntity extra2;

  private RelationshipType relationshipTypeBiDirectional;

  private RelationshipType relationshipTypeUniDirectional;

  @BeforeEach
  void setUp() {
    OrganisationUnit ou = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(ou);
    original = createTrackedEntity(ou);
    duplicate = createTrackedEntity(ou);
    extra1 = createTrackedEntity(ou);
    extra2 = createTrackedEntity(ou);
    manager.save(original);
    manager.save(duplicate);
    manager.save(extra1);
    manager.save(extra2);
    relationshipTypeBiDirectional = createRelationshipType('A');
    relationshipTypeUniDirectional = createRelationshipType('B');
    relationshipTypeBiDirectional.setBidirectional(true);
    relationshipTypeUniDirectional.setBidirectional(false);
    relationshipTypeService.addRelationshipType(relationshipTypeBiDirectional);
    relationshipTypeService.addRelationshipType(relationshipTypeUniDirectional);
  }

  @Test
  void moveSingleBiDirectionalRelationship() {
    Relationship bi1 = createTeToTeRelationship(original, extra2, relationshipTypeBiDirectional);
    Relationship bi2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeBiDirectional);
    Relationship bi3 = createTeToTeRelationship(duplicate, extra2, relationshipTypeBiDirectional);
    Relationship bi4 = createTeToTeRelationship(extra1, extra2, relationshipTypeBiDirectional);
    manager.save(bi1);
    manager.save(bi2);
    manager.save(bi3);
    manager.save(bi4);
    transactionTemplate.execute(
        status -> {
          List<String> relationships = Lists.newArrayList(bi2.getUid());
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship _bi1 = getRelationship(bi1.getUid());
          Relationship _bi2 = getRelationship(bi2.getUid());
          Relationship _bi3 = getRelationship(bi3.getUid());
          Relationship _bi4 = getRelationship(bi4.getUid());
          assertNotNull(_bi1);
          assertEquals(original.getUid(), _bi1.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi1.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi2);
          assertEquals(original.getUid(), _bi2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), _bi2.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi3);
          assertEquals(duplicate.getUid(), _bi3.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi3.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi4);
          assertEquals(extra1.getUid(), _bi4.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi4.getTo().getTrackedEntity().getUid());
          return null;
        });
  }

  @Test
  void moveSingleUniDirectionalRelationship() {
    Relationship uni1 = createTeToTeRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship uni3 = createTeToTeRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship uni4 = createTeToTeRelationship(extra1, extra2, relationshipTypeUniDirectional);
    manager.save(uni1);
    manager.save(uni2);
    manager.save(uni3);
    manager.save(uni4);
    original = manager.get(TrackedEntity.class, original.getUid());
    duplicate = manager.get(TrackedEntity.class, duplicate.getUid());
    List<String> relationships = Lists.newArrayList(uni3.getUid());
    potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
    manager.update(original);
    manager.update(duplicate);
    Relationship _uni1 = getRelationship(uni1.getUid());
    Relationship _uni2 = getRelationship(uni2.getUid());
    Relationship _uni3 = getRelationship(uni3.getUid());
    Relationship _uni4 = getRelationship(uni4.getUid());
    assertNotNull(_uni1);
    assertEquals(original.getUid(), _uni1.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), _uni1.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni2);
    assertEquals(duplicate.getUid(), _uni2.getFrom().getTrackedEntity().getUid());
    assertEquals(extra1.getUid(), _uni2.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni3);
    assertEquals(extra2.getUid(), _uni3.getFrom().getTrackedEntity().getUid());
    assertEquals(original.getUid(), _uni3.getTo().getTrackedEntity().getUid());
    assertNotNull(_uni4);
    assertEquals(extra1.getUid(), _uni4.getFrom().getTrackedEntity().getUid());
    assertEquals(extra2.getUid(), _uni4.getTo().getTrackedEntity().getUid());
  }

  @Test
  void moveMultipleRelationship() {
    Relationship uni1 = createTeToTeRelationship(original, extra2, relationshipTypeUniDirectional);
    Relationship uni2 = createTeToTeRelationship(duplicate, extra1, relationshipTypeUniDirectional);
    Relationship bi1 = createTeToTeRelationship(extra2, duplicate, relationshipTypeUniDirectional);
    Relationship bi2 = createTeToTeRelationship(extra1, extra2, relationshipTypeUniDirectional);
    manager.save(uni1);
    manager.save(uni2);
    manager.save(bi1);
    manager.save(bi2);
    transactionTemplate.execute(
        status -> {
          List<String> relationships = Lists.newArrayList(uni2.getUid(), bi1.getUid());
          potentialDuplicateStore.moveRelationships(original, duplicate, relationships);
          return null;
        });
    transactionTemplate.execute(
        status -> {
          dbmsManager.clearSession();
          Relationship _uni1 = getRelationship(uni1.getUid());
          Relationship _uni2 = getRelationship(uni2.getUid());
          Relationship _bi1 = getRelationship(bi1.getUid());
          Relationship _bi2 = getRelationship(bi2.getUid());
          assertNotNull(_uni1);
          assertEquals(original.getUid(), _uni1.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _uni1.getTo().getTrackedEntity().getUid());
          assertNotNull(_uni2);
          assertEquals(original.getUid(), _uni2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra1.getUid(), _uni2.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi1);
          assertEquals(extra2.getUid(), _bi1.getFrom().getTrackedEntity().getUid());
          assertEquals(original.getUid(), _bi1.getTo().getTrackedEntity().getUid());
          assertNotNull(_bi2);
          assertEquals(extra1.getUid(), _bi2.getFrom().getTrackedEntity().getUid());
          assertEquals(extra2.getUid(), _bi2.getTo().getTrackedEntity().getUid());
          return null;
        });
  }

  private Relationship getRelationship(String uid) {
    return manager.get(Relationship.class, uid);
  }
}
