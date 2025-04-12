/*
 * Copyright 2025 Aarol D'Souza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.cdap.wrangler.api.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Parser for time durations in the wrangler API.
 */
public class TimeDuration implements Token {
  private final String original;
  private final long millis;

  public TimeDuration(String value) {
    this.original = value;
    this.millis = parse(value);
  }

  /**
   * Parses the input string and converts it to milliseconds.
   * Supports units: ms, s, m, h.
   */
  private long parse(String input) {

    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input value cannot be null or empty.");
    }

    input = input.trim().toLowerCase(); // Normalize input

    double number;

    try {
        number = Double.parseDouble(input.replaceAll("[^\\d.]", ""));
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number format in input: " + input, e);
    }
    
    if (input.endsWith("ms")) {
      return (long) number;  // Milliseconds
    }
    if (input.endsWith("s")) {
      return (long) (number * 1000);  // Seconds to milliseconds
    }
    if (input.endsWith("m")) {
      return (long) (number * 60 * 1000);  // Minutes to milliseconds
    }
    if (input.endsWith("h")) {
      return (long) (number * 60 * 60 * 1000);  // Hours to milliseconds
    }
    
    // Throw exception or handle invalid units
    throw new IllegalArgumentException("Unknown time duration unit in input: " + input);
  }

  public long getMilliseconds() {
    return millis;
  }

  @Override
  public Object value() {
    return millis;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;  // Ensure TIME_DURATION is in the TokenType enum
  }

  @Override
  public com.google.gson.JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.add("type", new JsonPrimitive("TIME_DURATION"));
    json.add("original", new JsonPrimitive(original));
    json.add("milliseconds", new JsonPrimitive(millis));
    return json;
  }
}
