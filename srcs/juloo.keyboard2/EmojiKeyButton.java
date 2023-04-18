package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class EmojiKeyButton extends Button
  implements View.OnClickListener
{
  KeyValue _key;

  public EmojiKeyButton(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOnClickListener(this);
    String key_name = attrs.getAttributeValue(null, "k");
    _key = (key_name == null) ? null : KeyValue.getKeyByName(key_name);
    setText(_key.getString());
    if (_key.hasFlags(KeyValue.FLAG_KEY_FONT))
      setTypeface(Theme.getKeyFont(context));
  }

  public void onClick(View v)
  {
    Config config = Config.globalConfig();
    config.handler.key_up(_key, Pointers.Modifiers.EMPTY);
  }
}
