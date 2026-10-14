package juloo.keyboard2.staged;

import static org.junit.Assert.assertEquals;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Implement [Composer] so that the compiler checks its members.

    The plumbing alone cannot catch a broken declaration in the interface:
    nothing in the plumbing worktree implements [Composer], so an interface
    whose `candidates()` had been swallowed by an unterminated comment compiled
    there and only failed on the language branches that rebased onto it. Every
    method carries `@Override` so that the compiler rejects this test the moment
    the interface loses one.

    Suggested for the plumbing branch, at
    `test/juloo.keyboard2/staged/ComposerContractTest.java`. */
public class ComposerContractTest
{
  static final Composer EMPTY = new Composer()
  {
    @Override public boolean type(String s) { return false; }
    @Override public boolean backspace() { return false; }
    @Override public String text() { return ""; }
    @Override public List<Composer.Candidate> candidates()
    { return Collections.emptyList(); }
    @Override public void reset() {}
  };

  @Test
  public void an_empty_composer_offers_no_candidate()
  {
    assertEquals(0, EMPTY.candidates().size());
  }
}
