package juloo.keyboard2.dict;

import android.content.res.Resources;
import java.io.IOException;
import java.io.InputStream;
import juloo.cdict.Cdict;
import juloo.keyboard2.R;

public final class Dictionary
{
  public static Cdict main_fr;

  private static boolean inited = false;

  public static void init(Resources res)
  {
    if (inited)
      return;
    inited = true;
    try
    {
      InputStream input = res.openRawResource(R.raw.main_fr);
      main_fr = Cdict.of_bytes(input.readAllBytes());
    }
    catch (IOException _exn) {}
  }
}
