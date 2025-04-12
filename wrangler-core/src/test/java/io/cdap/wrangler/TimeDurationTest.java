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
package io.cdap.wrangler;

import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Test; // Correct import for @Test annotation
import static org.junit.Assert.assertEquals; // Correct static import for assertEquals

public class TimeDurationTest {

    @Test
    public void testParseMilliseconds() {
        TimeDuration duration = new TimeDuration("500ms");
        assertEquals(500, duration.getMilliseconds());
    }

    @Test
    public void testParseSeconds() {
        TimeDuration duration = new TimeDuration("2s");
        assertEquals(2000, duration.getMilliseconds());
    }

    @Test
    public void testParseMinutes() {
        TimeDuration duration = new TimeDuration("1m");
        assertEquals(60000, duration.getMilliseconds());
    }

    @Test
    public void testParseMinutes2() {
        TimeDuration duration = new TimeDuration("1.3m");
        assertEquals(78000, duration.getMilliseconds());
    }

    @Test
    public void testParseHours() {
        TimeDuration duration = new TimeDuration("1h");
        assertEquals(3600000, duration.getMilliseconds());
    }

    @Test
    public void testParseDecimalSeconds() {
        TimeDuration duration = new TimeDuration("1.5s");
        assertEquals(1500, duration.getMilliseconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidInput() {
        new TimeDuration("invalid"); // Invalid input, should throw exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmptyInput() {
        new TimeDuration(""); // Empty input, should throw exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullInput() {
        new TimeDuration(null); // Null input, should throw exception
    }
}
