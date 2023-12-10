package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.InputType;
import android.text.Layout;
import android.widget.EditText;

public class CustomLayoutEditDialog
{
  /** Dialog for specifying a custom layout. [initial_text] is the layout
      description when modifying a layout. */
  public static void show(Context ctx, String initial_text,
      boolean allow_remove, final Callback callback)
  {
    final LayoutEntryEditText input = new LayoutEntryEditText(ctx);
    input.setText(initial_text);
    AlertDialog.Builder dialog = new AlertDialog.Builder(ctx)
      .setView(input)
      .setTitle(R.string.pref_custom_layout_title)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _dialog, int _which)
        {
          callback.select(input.getText().toString());
        }
      })
      .setNegativeButton(android.R.string.cancel, null);
    // Might be true when modifying an existing layout
    if (allow_remove)
      dialog.setNeutralButton(R.string.pref_layouts_remove_custom, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface _dialog, int _which)
        {
          callback.select(null);
        }
      });
    dialog.show();
  }

  public interface Callback
  {
    /** The entered text when the user clicks "OK", [null] when the user
        cancels editing. */
    public void select(String text);
  }

  /** An editable text view that shows line numbers. */
  static class LayoutEntryEditText extends EditText
  {
    /** Used to draw line numbers. */
    Paint _ln_paint;

    public LayoutEntryEditText(Context ctx)
    {
      super(ctx);
      _ln_paint = new Paint(getPaint());
      _ln_paint.setTextSize(_ln_paint.getTextSize() * 0.8f);
      setHorizontallyScrolling(true);
      setInputType(InputType.TYPE_CLASS_TEXT
          | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
      float digit_width = _ln_paint.measureText("0");
      int line_count = getLineCount();
      // Extra '+ 1' serves as padding.
      setPadding((int)(((int)Math.log10(line_count) + 1 + 1) * digit_width), 0, 0, 0);
      super.onDraw(canvas);
      _ln_paint.setColor(getPaint().getColor());
      Rect clip_bounds = canvas.getClipBounds();
      Layout layout = getLayout();
      int offset = clip_bounds.left + (int)(digit_width / 2.f);
      int line = layout.getLineForVertical(clip_bounds.top);
      int skipped = line;
      while (line < line_count)
      {
        int baseline = getLineBounds(line, null);
        canvas.drawText(String.valueOf(line), offset, baseline, _ln_paint);
        line++;
        if (baseline >= clip_bounds.bottom)
          break;
      }
    }
  }
}
