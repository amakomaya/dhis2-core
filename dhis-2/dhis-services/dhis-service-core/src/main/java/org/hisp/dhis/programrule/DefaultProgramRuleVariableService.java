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
package org.hisp.dhis.programrule;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Program;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service("org.hisp.dhis.programrule.ProgramRuleVariableService")
public class DefaultProgramRuleVariableService implements ProgramRuleVariableService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final ProgramRuleVariableStore programRuleVariableStore;

  // -------------------------------------------------------------------------
  // ProgramRuleVariable implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramRuleVariable(ProgramRuleVariable programRuleVariable) {
    programRuleVariableStore.save(programRuleVariable);
    return programRuleVariable.getId();
  }

  @Override
  @Transactional
  public void deleteProgramRuleVariable(ProgramRuleVariable programRuleVariable) {
    programRuleVariableStore.delete(programRuleVariable);
  }

  @Override
  @Transactional
  public void updateProgramRuleVariable(ProgramRuleVariable programRuleVariable) {
    programRuleVariableStore.update(programRuleVariable);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramRuleVariable getProgramRuleVariable(long id) {
    return programRuleVariableStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleVariable> getAllProgramRuleVariable() {
    return programRuleVariableStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleVariable> getProgramRuleVariable(Program program) {
    return programRuleVariableStore.get(program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleVariable> getVariablesWithNoDataElement() {
    return programRuleVariableStore.getVariablesWithNoDataElement();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramRuleVariable> getVariablesWithNoAttribute() {
    return programRuleVariableStore.getVariablesWithNoAttribute();
  }
}
