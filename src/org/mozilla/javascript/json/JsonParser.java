/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Raphael Speyer
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript.json;

import org.mozilla.javascript.json.JsonLexer.Token;

import static org.mozilla.javascript.json.JsonLexer.Token.*;
import static org.mozilla.javascript.json.JsonLexer.VALUE_START_TOKENS;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * This class converts a stream of JSON tokens into a JSON value.
 *
 * See ECMA 15.12.
 * @author Raphael Speyer
 */

public class JsonParser {
  public JsonParser(Context cx, Scriptable scope) {
    this.cx = cx;
    this.scope = scope;
  }

  private Context cx;
  private Scriptable scope;

  private Object readNull(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer, NULL);
    lexer.moveNext();
    return null;
  }

  private Boolean readBoolean(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer, BOOLEAN);
    Boolean bool = BOOLEAN.evaluate(lexer.getLexeme());
    lexer.moveNext();
    return bool;
  }

  private Number readNumber(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer, NUMBER);
    Number num = NUMBER.evaluate(lexer.getLexeme());
    lexer.moveNext();
    return num;
  }

  private String readString(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer, STRING);
    String str = STRING.evaluate(lexer.getLexeme());
    lexer.moveNext();
    return str;
  }

  private Scriptable newObject() {
    return cx.newObject(scope);
  }

  private Scriptable readObject(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer, OPEN_BRACE);
    expectMoveNext(lexer, STRING, CLOSE_BRACE);
    Scriptable object = newObject();

    while (isCurrentToken(lexer, STRING)) {
      String string = readString(lexer);
      expectCurrentToken(lexer,COLON);
      expectMoveNext(lexer, VALUE_START_TOKENS);
      Object value = readValue(lexer);
      object.put(string, object, value);
      if (isCurrentToken(lexer, CLOSE_BRACE))
        break;
      else {
        expectCurrentToken(lexer,COMMA);
        expectMoveNext(lexer, VALUE_START_TOKENS);
      }
    }

    expectCurrentToken(lexer, CLOSE_BRACE);
    lexer.moveNext();

    return object;
  }

  private Scriptable newArray(List<Object> items) {
    return cx.newArray(scope, items.toArray());
  }

  private Scriptable readArray(JsonLexer lexer) throws ParseException {
    expectCurrentToken(lexer,OPEN_BRACKET);
    expectMoveNext(lexer, NULL, BOOLEAN, NUMBER, STRING, OPEN_BRACKET, OPEN_BRACE, CLOSE_BRACKET);

    List<Object> array = new ArrayList<Object>();

    while (isCurrentToken(lexer, VALUE_START_TOKENS)) {
      array.add(readValue(lexer));

      if (isCurrentToken(lexer, CLOSE_BRACKET)) {
        break;
      } else {
        expectCurrentToken(lexer,COMMA);
        expectMoveNext(lexer, VALUE_START_TOKENS);
      }
    }

    expectCurrentToken(lexer,CLOSE_BRACKET);
    lexer.moveNext();

    return newArray(array);
  }

  private Object readValue(JsonLexer lexer) throws ParseException {
    if (isCurrentToken(lexer, NULL)) {
      return readNull(lexer);
    } else if (isCurrentToken(lexer, BOOLEAN)) {
      return readBoolean(lexer);
    } else if (isCurrentToken(lexer, NUMBER)) {
      return readNumber(lexer);
    } else if (isCurrentToken(lexer, STRING)) {
      return readString(lexer);
    } else if (isCurrentToken(lexer, OPEN_BRACKET)) {
      return readArray(lexer);
    } else if (isCurrentToken(lexer, OPEN_BRACE)) {
      return readObject(lexer);
    } else {
      throw new ParseException(lexer.getLexeme(), VALUE_START_TOKENS);
    }
  }

  /**
   * Checks that the current token is <tt>expected</tt>, and if not throws a ParseException
   */
  private void expectCurrentToken(JsonLexer lexer, Token expected) throws ParseException {
    if (!isCurrentToken(lexer, expected)) {
      throw new ParseException(lexer.getLexeme(), expected);
    }
  }

  /**
   * Checks that the current token is one of <tt>expected</tt>, and if not throws a ParseException
   */
  private void expectCurrentToken(JsonLexer lexer, Token... expected) throws ParseException {
    if (!isCurrentToken(lexer, expected)) {
      throw new ParseException(lexer.getLexeme(), expected);
    }
  }

  /**
   * Attempts to move the lexer to the next token, and throws a ParseException with the expected next token types if not successful
   */
  private void expectMoveNext(JsonLexer lexer, Token... expected) throws ParseException {
    if (!lexer.moveNext()) {
      throw new ParseException("no valid tokens from " + lexer.getOffset(), expected);
    }
    expectCurrentToken(lexer, expected);
  }

  private boolean isCurrentToken(JsonLexer lexer, Token tok) {
    return lexer.getToken() == tok;
  }

  private boolean isCurrentToken(JsonLexer lexer, Token... toks) {
    for (Token tok : toks) {
      if (lexer.getToken() == tok)
        return true;
    }
    return false;
  }

  public static class ParseException extends Exception {

    private ParseException(String message) {
      super(message);
    }

    private ParseException(Throwable error) {
      super(error);
    }

    private ParseException(String found, Token... expected) {
      super(buildMessage(found, expected));
    }

    private static String buildMessage(String found, Token... expected) {
      StringBuffer buffer = new StringBuffer("Expected: ");

      for (int i = 0; i < expected.length; i++) {
        buffer.append(expected[i].toString());
        if (i < expected.length - 1)
          buffer.append(" or ");
      }

      buffer.append(" , Found: '" + found + "'");
      
      return buffer.toString();
    }
  }

  public Object parseValue(String json) throws ParseException {
    if (json == null) 
      throw new ParseException("Input JSON string may not be null");
    try {
      JsonLexer lexer = initLexer(json, VALUE_START_TOKENS);
      Object value = readValue(lexer);
      if (lexer.finished() && lexer.getLexeme().equals("")) 
        return value;
      else
        throw new ParseException("Expected end of stream at char "+lexer.getOffset());
    } catch (IllegalArgumentException iae) {
      throw new ParseException(iae);
    }
  }

  private JsonLexer initLexer(String json, Token... firstToken) throws ParseException {
    JsonLexer lexer = new JsonLexer(json);
    expectMoveNext(lexer, firstToken);
    return lexer;
  }
}
