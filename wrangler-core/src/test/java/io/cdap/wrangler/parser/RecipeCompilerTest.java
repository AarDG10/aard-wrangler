/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.CompileException;
import io.cdap.wrangler.api.CompileStatus;
import io.cdap.wrangler.api.Compiler;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;

import org.junit.Assert;
import org.junit.Test;


import java.util.List;
import java.util.Set;

/**
 * Tests {@link RecipeCompiler}
 */
public class RecipeCompilerTest {

  private static final Compiler compiler = new RecipeCompiler();

  @Test
  public void testSuccessCompilation() throws Exception {
    try {
      Compiler compiler = new RecipeCompiler();
      CompileStatus status = compiler.compile(
          "parse-as-csv :body ' ' true;\n"
        + "set-column :abc, :edf;\n"
        + "send-to-error exp:{ window < 10 } ;\n"
        + "parse-as-simple-date :col 'yyyy-mm-dd' :col 'test' :col2,:col4,:col9 10 exp:{test < 10};\n"
      );

      Assert.assertNotNull(status.getSymbols());
      Assert.assertEquals(4, status.getSymbols().size());
    } catch (CompileException e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testMacroSkippingDuringParsing() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv :body ',' true;",
      "${macro1}",
      "${macro${number}}",
      "parse-as-csv :body '${delimiter}' true;"
    };

    CompileStatus status = TestingRig.compile(recipe);
    Assert.assertEquals(true, status.isSuccess());
  }

  @Test
  public void testSingleMacroLikeWranglerPlugin() throws Exception {
    String[] recipe = new String[] {
      "${directives}"
    };

    CompileStatus status = TestingRig.compile(recipe);
    Assert.assertEquals(true, status.isSuccess());
  }

  @Test
  public void testSparedPragmaLoadDirectives() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives}",
      "#pragma load-directives root1,root2,root3;"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testNestedMacros() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives_${number}}"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testSemiColonMissing() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5",
      "${directives_${number}}"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingOpenBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "$directives}"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingCloseBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingBothBraceOnMacro() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4,test5;",
      "${directives"
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testMissingPragmaHash() throws Exception {
    String[] recipe = new String[] {
      "pragma load-directives test1,test2,test3,test4,test5;",
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testTypograhicalErrorPragmaLoadDirectives() throws Exception {
    String[] recipe = new String[] {
      "pragma test1,test2,test3,test4,test5;",
    };
    TestingRig.compileFailure(recipe);
  }

  @Test
  public void testWithIfStatement() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2;",
      "${macro_1}",
      "if ((test > 10) && (window < 20)) {  parse-as-csv :body ',' true; if (window > 10) " +
        "{ send-to-error exp:{test > 10}; } }"
    };
    TestingRig.compileSuccess(recipe);
  }

  @Test
  public void testComplexExpression() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv body , true",
      "drop body",
      "merge body_1 body_2 Full_Name ' '",
      "drop body_1,body_2",
      "find-and-replace body_4 s/Washington//g",
      "send-to-error empty(body_4)",
      "send-to-error body_5 =~ \"DC.*\"",
      "filter-rows-on regex-match body_5 *as*"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void test() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv body , true",
      "drop body",
      "merge body_1 body_2 Full_Name ' '",
      "drop body_1,body_2",
      "find-and-replace body_4 s/Washington//g",
      "send-to-error empty(body_4)",
      "send-to-error body_5 =~ \"DC.*\"",
      "filter-rows-on regex-match body_5 *as*"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testSingleLineDirectives() throws Exception {
    String[] recipe = new String[] {
      "parse-as-csv :body '\t' true; drop :body;"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testError() throws Exception {
    String[] recipe = new String[] {
      "parse-as-abababa-csv :body '\t' true; drop :body;"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Assert.assertTrue(true);
  }

  @Test
  public void testRecipePragmaWithCompiler() throws Exception {
    String[] recipe = new String[] {
      "#pragma load-directives test1,test2,test3,test4;",
      "${directives}"
    };
    CompileStatus compile = TestingRig.compile(recipe);
    Set<String> loadableDirectives = compile.getSymbols().getLoadableDirectives();
    Assert.assertEquals(4, loadableDirectives.size());
  }

  //Added here (further parser tests)

  @Test
  public void testTimeDurationTokenCompilation() throws Exception {
      String recipe = 
          "set-column :duration_col exp:{500ms};\n" +
          "set-column :duration_col2 exp:{2.5s};\n" +
          "set-column :duration_col3 exp:{1h};";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(3, status.getSymbols().size());
  }
  
  @Test
  public void testByteSizeTokenCompilation() throws Exception {
      String recipe = 
          "set-column :size_col exp:{1024KB};\n" +
          "set-column :size_col2 exp:{2MB};\n" +
          "set-column :size_col3 exp:{1GB};";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(3, status.getSymbols().size());
  }
  
  @Test
  public void testAggregateStatsDirectiveCompilation() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_size :response_time :total_size :total_time;";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(1, status.getSymbols().size());
  }
  
  @Test
  public void testAggregateStatsWithUnits() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec;";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(1, status.getSymbols().size());
  }
  
  @Test
  public void testAggregateStatsWithCustomUnits() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_size :response_time total_size_gb total_time_min;";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(1, status.getSymbols().size());
  }
  
  @Test(expected = CompileException.class)
  public void testAggregateStatsWithInvalidInputColumn() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :invalid_column :response_time total_size total_time;";
      
      compiler.compile(recipe);
  }
  
  @Test(expected = CompileException.class)
  public void testAggregateStatsWithInvalidOutputColumn() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_size :response_time invalid_output_name total_time;";
      
      compiler.compile(recipe);
  }
  
  @Test(expected = CompileException.class)
  public void testAggregateStatsWithMissingInputColumns() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats total_size total_time;"; // Missing input columns
      
      compiler.compile(recipe);
  }
  
  @Test(expected = CompileException.class)
  public void testAggregateStatsWithMissingOutputColumns() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_size :response_time;"; // Missing output columns
      
      compiler.compile(recipe);
  }
  
  @Test
  public void testComplexRecipeWithNewTokens() throws Exception {
      String recipe = 
          "parse-as-csv :body ',' true;\n" +
          "set-column :file_size exp:{2.5GB};\n" +
          "set-column :process_time exp:{1.5m};\n" +
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :file_size :process_time :total_size :total_time;";
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(4, status.getSymbols().size());
  }
  
  @Test(expected = CompileException.class)
  public void testInvalidByteSizeCompilation() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "set-column :size_col exp:{ invalid_size = 10XB };\n"; // Invalid byte unit
      compiler.compile(recipe);
  }
  
  @Test(expected = CompileException.class)
  public void testInvalidTimeDurationCompilation() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "set-column :duration_col exp:{ invalid_time = 5y };\n"; // Invalid time unit
      compiler.compile(recipe);
  }
  
  @Test(expected = CompileException.class)
  public void testMissingArgumentsInAggregateStats() throws Exception {
      String recipe = 
          "#pragma load-directives aggregate-stats;\n" +
          "aggregate-stats :data_size;\n"; // Missing required arguments
      compiler.compile(recipe);
  }
  
  @Test
  public void testCaseInsensitiveUnits() throws Exception {
      String recipe = 
          "set-column :size1 exp:{1kb};\n" +   // Testing lowercase KB
          "set-column :size2 exp:{1MB};\n" +   // Testing uppercase MB
          "set-column :time1 exp:{1MS};\n" +   // Testing uppercase MS
          "set-column :time2 exp:{1h};";       // Testing lowercase h
      
      CompileStatus status = compiler.compile(recipe);
      Assert.assertTrue(status.isSuccess());
      Assert.assertEquals(4, status.getSymbols().size());
  }
  @Test
public void testValidByteSizeCompilation() throws Exception {
    String recipe = 
        "#pragma load-directives aggregate-stats;\n" +
        "set-column :size_col exp:{ valid_size = 10KB };\n" + // Valid byte unit
        "set-column :size_col2 exp:{ valid_size = 1MB };\n" + // Valid byte unit
        "set-column :size_col3 exp:{ valid_size = 2GB };"; // Valid byte unit
    CompileStatus status = compiler.compile(recipe);
    Assert.assertTrue(status.isSuccess());
}

@Test
public void testValidTimeDurationCompilation() throws Exception {
    String recipe = 
        "#pragma load-directives aggregate-stats;\n" +
        "set-column :duration_col exp:{ valid_time = 100ms };\n" + // Valid time unit
        "set-column :duration_col2 exp:{ valid_time = 5s };\n" + // Valid time unit
        "set-column :duration_col3 exp:{ valid_time = 2m };\n" + // Valid time unit
        "set-column :duration_col4 exp:{ valid_time = 1h };"; // Valid time unit
    CompileStatus status = compiler.compile(recipe);
    Assert.assertTrue(status.isSuccess());
}

@Test
public void testValidAggregateStats() throws Exception {
    String recipe = 
        "#pragma load-directives aggregate-stats;\n" +
        "aggregate-stats :data_size :response_time :total_size :total_time;"; // All required arguments
    CompileStatus status = compiler.compile(recipe);
    Assert.assertTrue(status.isSuccess());
}
}
