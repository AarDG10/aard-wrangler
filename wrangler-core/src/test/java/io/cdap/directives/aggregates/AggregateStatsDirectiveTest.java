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

package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.parser.MapArguments;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.DirectiveName;
import io.cdap.wrangler.api.parser.TokenDefinition;
import io.cdap.wrangler.api.TokenGroup;
import io.cdap.wrangler.api.parser.TokenType;

public class AggregateStatsDirectiveTest {

    private AggregateStatsDirective directive;
    
    @Mock
    private ExecutorContext context;
    
    @Mock
    private TransientStore store;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        directive = new AggregateStatsDirective();
        when(context.getTransientStore()).thenReturn(store);
    }

    @Test
    public void testUsageDefinition() {
        UsageDefinition define = directive.define();
        Assert.assertNotNull(define);
        List<TokenDefinition> tokens = define.getTokens();
        Assert.assertTrue(tokens.stream().anyMatch(t -> t.name().equals("byteSizeColumn")));
        Assert.assertTrue(tokens.stream().anyMatch(t -> t.name().equals("timeDurationColumn")));
        Assert.assertTrue(tokens.stream().anyMatch(t -> t.name().equals("totalSizeColumn")));
        Assert.assertTrue(tokens.stream().anyMatch(t -> t.name().equals("totalTimeColumn")));
    }

    @Test
    public void testBasicAggregation() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create test data
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", "1KB").add("time", "100ms"));
        rows.add(new Row("size", "2MB").add("time", "1.5s"));
        rows.add(new Row("size", "500KB").add("time", "750ms"));

        // Execute directive
        List<Row> results = directive.execute(rows, context);

        // Verify results
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);

        // Expected: ~2.5MB (1KB + 2MB + 500KB converted to bytes)
        double expectedSizeBytes = 1024 + 2 * 1024 * 1024 + 500 * 1024;
        Assert.assertEquals(expectedSizeBytes, (Double) result.getValue("total_size"), 0.001);

        // Expected: 2350ms (100ms + 1500ms + 750ms)
        double expectedTimeMs = 100 + 1500 + 750;
        Assert.assertEquals(expectedTimeMs, (Double) result.getValue("total_time"), 0.001);
    }

    @Test
    public void testMixedUnitAggregation() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create test data with mixed units
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", "1GB").add("time", "1m"));
        rows.add(new Row("size", "500MB").add("time", "30s"));
        rows.add(new Row("size", "250MB").add("time", "15s"));

        // Execute directive
        List<Row> results = directive.execute(rows, context);

        // Verify results
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);

        // Expected: Convert all to bytes
        // 1GB = 1024 * 1024 * 1024 bytes
        // 500MB = 500 * 1024 * 1024 bytes
        // 250MB = 250 * 1024 * 1024 bytes
        double expectedSizeBytes = (1024L * 1024L * 1024L) + (500L * 1024L * 1024L) + (250L * 1024L * 1024L);
        Assert.assertEquals(expectedSizeBytes, (Double) result.getValue("total_size"), 0.001);

        // Expected: Convert all to milliseconds
        // 1m = 60000ms
        // 30s = 30000ms
        // 15s = 15000ms
        double expectedTimeMs = 60000 + 30000 + 15000;
        Assert.assertEquals(expectedTimeMs, (Double) result.getValue("total_time"), 0.001);
    }

    @Test
    public void testEmptyInput() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create empty test data
        List<Row> rows = new ArrayList<>();

        // Execute directive
        List<Row> results = directive.execute(rows, context);

        // Verify results
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals(0.0, (Double) result.getValue("total_size"), 0.001);
        Assert.assertEquals(0.0, (Double) result.getValue("total_time"), 0.001);
    }

    @Test(expected = ErrorRowException.class)
    public void testInvalidByteSize() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create test data with invalid byte size
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", "invalid").add("time", "100ms"));

        // This should throw an ErrorRowException
        directive.execute(rows, context);
    }

    @Test(expected = ErrorRowException.class)
    public void testInvalidTimeDuration() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create test data with invalid time duration
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", "1KB").add("time", "invalid"));

        // This should throw an ErrorRowException
        directive.execute(rows, context);
    }

    @Test
    public void testNullValues() throws DirectiveParseException, DirectiveExecutionException, ErrorRowException {
        // Initialize directive
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
        builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
        builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
        builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
        builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
        
        TokenGroup tokenGroup = new TokenGroup();
        // Add tokens to group
        tokenGroup.add(new DirectiveName("aggregatestats"));
        tokenGroup.add(new ColumnName("size"));
        tokenGroup.add(new ColumnName("time"));
        tokenGroup.add(new ColumnName("total_size"));
        tokenGroup.add(new ColumnName("total_time"));

        Arguments args = new MapArguments(builder.build(), tokenGroup);
        directive.initialize(args);

        // Create test data with null values
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("size", null).add("time", "100ms"));
        rows.add(new Row("size", "1KB").add("time", null));
        rows.add(new Row("size", "2KB").add("time", "200ms"));

        // Execute directive
        List<Row> results = directive.execute(rows, context);

        // Verify results - should only include the valid row
        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals(2048.0, (Double) result.getValue("total_size"), 0.001); // 2KB in bytes
        Assert.assertEquals(200.0, (Double) result.getValue("total_time"), 0.001);
    }
} 