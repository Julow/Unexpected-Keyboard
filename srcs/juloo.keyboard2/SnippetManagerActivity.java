package juloo.keyboard2;

import android.app.Activity;
import android.os.Bundle;

public class SnippetManagerActivity extends Activity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    if (Config.globalConfig() != null)
    {
      setTheme(Config.globalConfig().theme);
    }
    else
    {
      setTheme(R.style.Dark);
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_snippet_manager);
  }
}
