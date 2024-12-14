package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;

final class ClipboardHistoryCheckBox extends CheckBox
  implements CompoundButton.OnCheckedChangeListener
{
  public ClipboardHistoryCheckBox(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setChecked(Config.globalConfig().clipboard_history_enabled);
    setOnCheckedChangeListener(this);
  }

  @Override
  public void onCheckedChanged(CompoundButton _v, boolean isChecked)
  {
    ClipboardHistoryService.set_history_enabled(isChecked);
  }
}
