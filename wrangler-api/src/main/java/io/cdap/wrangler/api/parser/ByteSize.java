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

import com.google.gson.JsonElement; //Needed to sorted alphabetically (i.e. alphabetically sorted importing)
import com.google.gson.JsonObject;
  
/**
 * Parser for byte size values like "10MB", "2GB", etc.
 */
public class ByteSize implements Token {
  private final String original;
  private final long bytes;

  public ByteSize(String value) {  // Take string as input like 10MB, etc...
    this.original = value;
    this.bytes = parse(value);  // Store in var (bytes)
  }

  /**
   * Parses the input string and converts it to bytes.
   * Supports units: KB, MB, GB.
   */
  private long parse(String input) {

    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input value cannot be null or empty.");
    }

    input = input.trim().toUpperCase(); // Normalize input

    double number;

    try {
        number = Double.parseDouble(input.replaceAll("[^\\d.]", ""));
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number format in input: " + input, e);
    }
    
    if (input.endsWith("KB")) {
      return (long) (number * 1024);
    }
    if (input.endsWith("MB")) {
      return (long) (number * 1024 * 1024);
    }
    if (input.endsWith("GB")) {
      return (long) (number * 1024 * 1024 * 1024);
    }
    
    // Throw exception or handle invalid units
    throw new IllegalArgumentException("Unknown byte size unit in input: " + input);
  }

  public long getBytes() {
    return bytes;
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("original", original);
    json.addProperty("bytes", bytes);
    return json;
  }
}
