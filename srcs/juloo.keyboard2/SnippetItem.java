package juloo.keyboard2;

import java.util.UUID;

public abstract class SnippetItem
{
  public final String uuid;
  public String name; // Label for snippet, Name for folder

  protected SnippetItem(String name)
  {
    this.uuid = UUID.randomUUID().toString();
    this.name = name;
  }

  protected SnippetItem(String uuid, String name)
  {
    this.uuid = uuid;
    this.name = name;
  }
}
