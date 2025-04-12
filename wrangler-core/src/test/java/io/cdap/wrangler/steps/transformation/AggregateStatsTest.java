/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.steps.transformation;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link AggregateStats}
 */
public class AggregateStatsTest {

  @Test
  public void testBasicAggregation() throws Exception {
    // Create test data
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_transfer_size", "1KB").add("response_time", "100ms"));
    rows.add(new Row("data_transfer_size", "2MB").add("response_time", "1.5s"));
    rows.add(new Row("data_transfer_size", "500KB").add("response_time", "750ms"));

    String[] recipe = new String[] {
      "#pragma load-directives aggregate-stats;",
      "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);

    // Expected: ~2.5MB (1KB + 2MB + 500KB converted to MB)
    // 1KB = 1024 bytes
    // 2MB = 2 * 1024 * 1024 bytes
    // 500KB = 500 * 1024 bytes
    double expectedSizeMB = (1024.0 + 2 * 1024 * 1024 + 500 * 1024) / (1024 * 1024);
    Assert.assertEquals(expectedSizeMB, (Double) result.getValue("total_size_mb"), 0.001);

    // Expected: ~2.35 seconds (100ms + 1.5s + 750ms converted to seconds)
    double expectedTimeSec = (0.1 + 1.5 + 0.75);
    Assert.assertEquals(expectedTimeSec, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testAggregationWithDifferentUnits() throws Exception {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_size", "1GB").add("response_time", "1m"));
    rows.add(new Row("data_size", "500MB").add("response_time", "30s"));
    rows.add(new Row("data_size", "250MB").add("response_time", "15s"));

    String[] recipe = new String[] {
      "#pragma load-directives aggregate-stats;",
      "aggregate-stats :data_size :response_time total_size_gb total_time_min"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);

    // Expected: ~1.75GB (1GB + 500MB + 250MB converted to GB)
    double expectedSizeGB = 1.0 + 500.0/1024 + 250.0/1024;
    Assert.assertEquals(expectedSizeGB, (Double) result.getValue("total_size_gb"), 0.001);

    // Expected: ~1.75 minutes (1m + 30s + 15s converted to minutes)
    double expectedTimeMin = 1.0 + 30.0/60 + 15.0/60;
    Assert.assertEquals(expectedTimeMin, (Double) result.getValue("total_time_min"), 0.001);
  }

  @Test
  public void testAggregationWithEmptyInput() throws Exception {
    List<Row> rows = new ArrayList<>();

    String[] recipe = new String[] {
      "#pragma load-directives aggregate-stats;",
      "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(0.0, (Double) result.getValue("total_size_mb"), 0.001);
    Assert.assertEquals(0.0, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testAggregationWithInvalidValues() throws Exception {
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_size", "invalid").add("response_time", "100ms"));
    rows.add(new Row("data_size", "1MB").add("response_time", "invalid"));

    String[] recipe = new String[] {
      "#pragma load-directives aggregate-stats;",
      "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
    };

    List<Row> results = TestingRig.execute(recipe, rows);

    // Should skip invalid values and only aggregate valid ones
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(1.0, (Double) result.getValue("total_size_mb"), 0.001);
    Assert.assertEquals(0.1, (Double) result.getValue("total_time_sec"), 0.001);
  }
} 