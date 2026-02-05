package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;

public class SnippetFolder extends SnippetItem
{
  public final List<SnippetItem> items;

  public SnippetFolder(String name)
  {
    super(name);
    this.items = new ArrayList<>();
  }

  public SnippetFolder(String uuid, String name)
  {
    super(uuid, name);
    this.items = new ArrayList<>();
  }

  public void addItem(SnippetItem item)
  {
    items.add(item);
  }

  public void removeItem(SnippetItem item)
  {
    items.remove(item);
  }

  public void removeItem(int index)
  {
    if (index >= 0 && index < items.size())
    {
      items.remove(index);
    }
  }
}
