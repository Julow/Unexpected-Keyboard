package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
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
    input.setTextSize(12.0f);
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
    input.set_on_text_change(new LayoutEntryEditText.OnChangeListener()
    {
      public void on_change()
      {
        String error = callback.validate(input.getText().toString());
        input.setError(error);
      }
    });
    dialog.show();
  }

  public interface Callback
  {
    /** The entered text when the user clicks "OK", [null] when the user
        cancels editing. */
    public void select(String text);

    /** Return a human readable error string if the [text] contains an error.
        Return [null] otherwise. The error string will be displayed atop the
        input box. This method is called everytime the text changes. */
    public String validate(String text);
  }

  /** An editable text view that shows line numbers. */
  static class LayoutEntryEditText extends EditText
  {
    /** Used to draw line numbers. */
    Paint _ln_paint;
    OnChangeListener _on_change_listener = null;

    /** Delay validation to when user stops typing for a second. */
    Handler _on_change_throttler;
    Runnable _on_change_delayed = new Runnable()
    {
      public void run()
      {
        OnChangeListener l = LayoutEntryEditText.this._on_change_listener;
        if (l != null)
          l.on_change();
      }
    };

    public LayoutEntryEditText(Context ctx)
    {
      super(ctx);
      _ln_paint = new Paint(getPaint());
      _ln_paint.setTextSize(_ln_paint.getTextSize() * 0.8f);
      setHorizontallyScrolling(true);
      setInputType(InputType.TYPE_CLASS_TEXT
          | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
      _on_change_throttler = new Handler(ctx.getMainLooper());
    }

    public void set_on_text_change(OnChangeListener l)
    {
      _on_change_listener = l;
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

    @Override
    protected void onTextChanged(CharSequence text, int _start, int _lengthBefore, int _lengthAfter)
    {
      if (_on_change_throttler != null)
      {
        _on_change_throttler.removeCallbacks(_on_change_delayed);
        _on_change_throttler.postDelayed(_on_change_delayed, 1000);
      }
    }

    public static interface OnChangeListener
    {
      public void on_change();
    }
  }
}
