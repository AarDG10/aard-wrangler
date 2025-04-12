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

import org.junit.Test;                  // Correct import for @Test annotation
import static org.junit.Assert.assertEquals; // Correct static import for assertEquals

public class ByteSizeTest {
    @Test
    public void testParseByteSizeKB() {
        ByteSize byteSize = new ByteSize("10KB");
        assertEquals(10240, byteSize.getBytes());
    }

    @Test
    public void testParseByteSizeMB() {
        ByteSize byteSize = new ByteSize("1.5MB");
        assertEquals(1572864, byteSize.getBytes());
    }

    @Test
    public void testParseByteSizeGB() {
        ByteSize byteSize = new ByteSize("2GB");
        assertEquals(2147483648L, byteSize.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidByteSizeUnit() {
        new ByteSize("10TB"); // Invalid unit, should throw exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidByteSizeFormat() {
        new ByteSize("abc123"); // Invalid format, should throw exception
    }

    @Test
    public void testCanonicalValueRetrieval() {
        ByteSize byteSize = new ByteSize("10MB");
        assertEquals(10485760, byteSize.getBytes()); // 10MB in bytes
    }
}
