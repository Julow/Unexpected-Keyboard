package srcs.res;

import com.android.ide.common.vectordrawable.Svg2Vector;
import java.io.File;
import java.io.FileOutputStream;

/** Inspired from Bernard Ladenthin's answer:
    https://stackoverflow.com/a/78898372 */
public class SvgToVector
{
  public static void main(String[] args)
  {
    if (args.length != 2)
    {
      System.out.println("Usage: svg_to_vector <input_file> <output_file>");
      return;
    }
    try
    {
      File input_file = new File(args[0]);
      FileOutputStream output_stream = new FileOutputStream(args[1]);
      String warnings;
      warnings = Svg2Vector.parseSvgToXml(input_file, output_stream);
      System.err.println(warnings);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(2);
    }
  }
}
