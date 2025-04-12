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
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements the AggregateStatsDirective, which is responsible for performing
 * aggregation operation and returning a single new Row containing the target columns with the 
calculated aggregate values (e.g., total_size_mb, total_time_sec)
 */
public class AggregateStatsDirective implements Directive {

    private String byteSizeColumn;
    private String timeDurationColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;

    // Initialize the totals in Store
    private static final String TOTAL_SIZE_KEY = "total_size";
    private static final String TOTAL_TIME_KEY = "total_time";
    
    @Override
    public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregatestats");
    builder.define("byteSizeColumn", TokenType.COLUMN_NAME);         // Required
    builder.define("timeDurationColumn", TokenType.COLUMN_NAME);     // Required
    builder.define("totalSizeColumn", TokenType.COLUMN_NAME);        // Required
    builder.define("totalTimeColumn", TokenType.COLUMN_NAME);        // Required

    return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
    this.byteSizeColumn = args.value("byteSizeColumn");
    this.timeDurationColumn = args.value("timeDurationColumn");
    this.totalSizeColumn = args.value("totalSizeColumn");
    this.totalTimeColumn = args.value("totalTimeColumn");
    }

    @Override
public List<Row> execute(List<Row> rows, ExecutorContext context)
        throws DirectiveExecutionException, ErrorRowException {

    double totalSize = 0;
    double totalTime = 0;

    for (Row row : rows) {
        try {
            Object byteSizeObj = row.getValue(byteSizeColumn);
            Object timeDurationObj = row.getValue(timeDurationColumn);

            if (byteSizeObj != null && timeDurationObj != null) {
                double byteSize;
                double timeDuration;

                // Handle string or numeric byte sizes
                if (byteSizeObj instanceof String) {
                    byteSize = new ByteSize((String) byteSizeObj).getBytes(); // returns long
                } else if (byteSizeObj instanceof Number) {
                    byteSize = ((Number) byteSizeObj).doubleValue();
                } else {
                    throw new IllegalArgumentException("Unsupported byteSize format: " + byteSizeObj);
                }

                // Handle string or numeric time durations
                if (timeDurationObj instanceof String) {
                    timeDuration = new TimeDuration((String) timeDurationObj).getMilliseconds(); // in ms
                } else if (timeDurationObj instanceof Number) {
                    timeDuration = ((Number) timeDurationObj).doubleValue();
                } else {
                    throw new IllegalArgumentException("Unsupported timeDuration format: " + timeDurationObj);
                }

                // Accumulate totals
                totalSize += byteSize;
                totalTime += timeDuration;
            }

        } catch (Exception e) {
            throw new ErrorRowException(
                "Error processing row: " + row, 
                1001,
                false,
                e
            );
        }
    }

    TransientStore transientStore = context.getTransientStore();
    transientStore.set(TransientVariableScope.GLOBAL, TOTAL_SIZE_KEY, totalSize);
    transientStore.set(TransientVariableScope.GLOBAL, TOTAL_TIME_KEY, totalTime);

    Row resultRow = new Row();
    resultRow.add(totalSizeColumn, totalSize);
    resultRow.add(totalTimeColumn, totalTime);

    List<Row> resultList = new ArrayList<>();
    resultList.add(resultRow);

    return resultList;
    }

    @Override
    public void destroy() {
    }
}
