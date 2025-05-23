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
package org.hisp.dhis.parser.expression.function;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.antlr.AntlrParserUtils.compare;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.List;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;

/**
 * Function is
 *
 * @author Jim Grace
 */
public class FunctionIs implements ExpressionItem {
  @Override
  public Object evaluate(ExprContext ctx, CommonExpressionVisitor visitor) {
    Object arg0 = visitor.visit(ctx.expr(0));

    for (int i = 1; i < ctx.expr().size(); i++) {
      Object argN = visitor.visit(ctx.expr(i));

      if (compare(arg0, argN) == 0) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Object evaluateAllPaths(ExprContext ctx, CommonExpressionVisitor visitor) {
    List<Object> values = ctx.expr().stream().map(visitor::visit).collect(toList());

    Object arg0 = values.get(0);

    for (int i = 1; i < values.size(); i++) {
      Object argN = values.get(i);

      if (compare(arg0, argN) == 0) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    List<String> args = ctx.expr().stream().map(visitor::castStringVisit).collect(toList());

    String arg0 = args.get(0);

    String others = String.join(",", args.subList(1, args.size()));

    return arg0 + " in (" + others + ")";
  }
}
