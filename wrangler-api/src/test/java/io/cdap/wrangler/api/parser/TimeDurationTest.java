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

public class TimeDurationTest {
    @Test
    public void testParseTimeDurationMs() {
        TimeDuration timeDuration = new TimeDuration("5ms");
        assertEquals(5, timeDuration.getMilliseconds());
    }

    @Test
    public void testParseTimeDurationS() {
        TimeDuration timeDuration = new TimeDuration("2.1s");
        assertEquals(2100, timeDuration.getMilliseconds());
    }

    @Test
    public void testParseTimeDurationM() {
        TimeDuration timeDuration = new TimeDuration("3m");
        assertEquals(180000, timeDuration.getMilliseconds());
    }

    @Test
    public void testParseTimeDurationH() {
        TimeDuration timeDuration = new TimeDuration("1h");
        assertEquals(3600000, timeDuration.getMilliseconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTimeDurationUnit() {
        new TimeDuration("10xy"); // Invalid unit, should throw exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTimeDurationFormat() {
        new TimeDuration("xyz"); // Invalid format, should throw exception
    }

    @Test
    public void testCanonicalValueRetrieval() {
        TimeDuration timeDuration = new TimeDuration("1.5h");
        assertEquals(5400000, timeDuration.getMilliseconds()); // 1.5 hours in milliseconds
    }
}
