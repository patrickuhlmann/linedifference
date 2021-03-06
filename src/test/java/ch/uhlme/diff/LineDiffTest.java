package ch.uhlme.diff;

import static ch.uhlme.matchers.FileContentIs.fileContentIs;
import static ch.uhlme.preparation.PrepareFile.prepareEmptyFile;
import static ch.uhlme.preparation.PrepareFile.prepareFileWithLines;
import static org.hamcrest.MatcherAssert.assertThat;

import ch.uhlme.BaseTest;
import ch.uhlme.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.DataflowAnomalyAnalysis",
    "PMD.AvoidDuplicateLiterals"
})
class LineDiffTest extends BaseTest {
  @SuppressWarnings("unused")
  @TempDir
  Path tempDir;

  @SuppressWarnings({"PMD.CloseResource"})
  @Test
  @DisplayName("exception if any input or output is null")
  void givenNullArguments_thenThrowException() throws IOException {
    Path firstInputPath = prepareEmptyFile(tempDir);
    Path secondInputPath = prepareEmptyFile(tempDir);
    Path outputPath = tempDir.resolve("outputNullArguments");
    InputFile firstInput = new InputFile(firstInputPath);
    InputFile secondInput = new InputFile(secondInputPath);
    OutputFolder output1 = new OutputFolder(outputPath);

    Assertions.assertThrows(
        NullPointerException.class, () -> new LineDiff(null, secondInput, output1));
    FileUtils.deleteRecursive(outputPath);

    OutputFolder output2 = new OutputFolder(outputPath);
    Assertions.assertThrows(
        NullPointerException.class, () -> new LineDiff(firstInput, null, output2));
    FileUtils.deleteRecursive(outputPath);

    OutputFolder output3 = new OutputFolder(outputPath);
    Assertions.assertThrows(
        NullPointerException.class, () -> new LineDiff(firstInput, secondInput, null));

    Assertions.assertDoesNotThrow(
        () -> {
          new LineDiff(firstInput, secondInput, output3);
        });
  }

  @Test
  @DisplayName("diff with empty input files")
  void givenEmptyInputFiles_thenDiff() throws IOException {
    runWithInputs(null, null);

    verifyBothFirstSecond(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
  }

  @Test
  @DisplayName("diff with empty second inputfile")
  void givenEmptySecondInput_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "b"), null);

    verifyBothFirstSecond(new LinkedList<>(), Arrays.asList("a", "b"), new LinkedList<>());
  }

  @Test
  @DisplayName("diff with empty first inputfile")
  void givenEmptyFirstInput_thenDiff() throws IOException {
    runWithInputs(null, Arrays.asList("a", "b"));

    verifyBothFirstSecond(new LinkedList<>(), new LinkedList<>(), Arrays.asList("a", "b"));
  }

  @Test
  @DisplayName("diff in the middle")
  void givenDiffMiddle_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "b", "d"), Arrays.asList("a", "c", "d"));

    verifyBothFirstSecond(
        Arrays.asList("a", "d"), Collections.singletonList("b"), Collections.singletonList("c"));
  }

  @Test
  @DisplayName("diff at start and end")
  void givenDiffStartEnd_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "c", "d"), Arrays.asList("b", "c", "e"));

    verifyBothFirstSecond(
        Collections.singletonList("c"), Arrays.asList("a", "d"), Arrays.asList("b", "e"));
  }

  @Test
  @DisplayName("diff if completely different")
  void givenCompleteDiff_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "c", "e"), Arrays.asList("b", "d", "f"));

    verifyBothFirstSecond(
        new LinkedList<>(), Arrays.asList("a", "c", "e"), Arrays.asList("b", "d", "f"));
  }

  @Test
  @DisplayName("diff with first input larger")
  void givenFirstLager_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "b", "c", "d", "e", "f"), Arrays.asList("a", "b", "f"));

    verifyBothFirstSecond(
        Arrays.asList("a", "b", "f"), Arrays.asList("c", "d", "e"), new LinkedList<>());
  }

  @Test
  @DisplayName("diff with second input larger")
  void givenSecondLarger_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "b", "f"), Arrays.asList("a", "b", "c", "d", "e", "f"));

    verifyBothFirstSecond(
        Arrays.asList("a", "b", "f"), new LinkedList<>(), Arrays.asList("c", "d", "e"));
  }

  @Test
  @DisplayName("diff with duplicate lines")
  void givenDuplicateLines_thenDiff() throws IOException {
    runWithInputs(Arrays.asList("a", "b", "b", "b", "c"), Arrays.asList("a", "b", "c", "d", "e"));

    verifyBothFirstSecond(
        Arrays.asList("a", "b", "c"), new LinkedList<>(), Arrays.asList("d", "e"));
  }

  @Test
  @DisplayName("diff with empty lines")
  void givenEmptyLines_thenDiff() throws IOException {
    runWithInputs(
        Arrays.asList("", "", "", "a", "b", "b", "b", "c"),
        Arrays.asList("", "a", "b", "c", "d", "e"));

    verifyBothFirstSecond(
        Arrays.asList("", "a", "b", "c"), new LinkedList<>(), Arrays.asList("d", "e"));
  }

  private void runWithInputs(List<String> firstInput, List<String> secondInput) throws IOException {
    Path firstPath =
        firstInput == null ? prepareEmptyFile(tempDir) : prepareFileWithLines(tempDir, firstInput);
    Path secondPath =
        secondInput == null
            ? prepareEmptyFile(tempDir)
            : prepareFileWithLines(tempDir, secondInput);
    Path output = tempDir.resolve("output");

    LineDiff diff =
        new LineDiff(new InputFile(firstPath), new InputFile(secondPath), new OutputFolder(output));
    diff.process();
  }

  private void verifyBothFirstSecond(
      List<String> elementsBoth, List<String> elementsFirst, List<String> elementsSecond) {
    assertThat(
        Paths.get(tempDir.resolve("output").toString(), "both.txt"), fileContentIs(elementsBoth));
    assertThat(
        Paths.get(tempDir.resolve("output").toString(), "first_only.txt"),
        fileContentIs(elementsFirst));
    assertThat(
        Paths.get(tempDir.resolve("output").toString(), "second_only.txt"),
        fileContentIs(elementsSecond));
  }
}
