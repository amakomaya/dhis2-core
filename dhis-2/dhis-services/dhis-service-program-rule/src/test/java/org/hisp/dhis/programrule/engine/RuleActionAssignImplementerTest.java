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
package org.hisp.dhis.programrule.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */
class RuleActionAssignImplementerTest extends DhisConvenienceTest {
  public static final String PI_UI = "pi-uid";

  private ProgramInstance programInstance;

  private RuleVariableInMemoryMap inMemoryMap;

  private RuleActionAssignValueImplementer assignValueImplementer;

  @BeforeEach
  public void initTest() {
    programInstance = new ProgramInstance();
    programInstance.setUid(PI_UI);

    inMemoryMap = new RuleVariableInMemoryMap();
    assignValueImplementer = new RuleActionAssignValueImplementer(inMemoryMap);
  }

  @Test
  void testRuleActionAssignRegex1() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1", RuleActionAssign.create("content", "action-data", "field"), "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("field"));
  }

  @Test
  void testRuleActionAssignRegex2() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1", RuleActionAssign.create("content", "action-data", "field123"), "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("field123"));
  }

  @Test
  void testRuleActionAssignRegex3() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1",
            RuleActionAssign.create("content", "action-data", "name-field"),
            "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("name-field"));
  }

  @Test
  void testRuleActionAssignRegex4() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1",
            RuleActionAssign.create("content", "action-data", "name field"),
            "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("name field"));
  }

  @Test
  void testRuleActionAssignRegex5() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1",
            RuleActionAssign.create("content", "action-data", "name.field"),
            "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("name.field"));
  }

  @Test
  void testRuleActionAssignRegex6() {
    assignValueImplementer.implement(
        RuleEffect.create(
            "ruleId1",
            RuleActionAssign.create("content", "action-data", "first name field"),
            "field-data"),
        programInstance);

    assertTrue(inMemoryMap.get(PI_UI).containsKey("first name field"));
  }
}
