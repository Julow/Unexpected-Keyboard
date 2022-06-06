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
    _key = KeyValue.getKeyByName(attrs.getAttributeValue(null, "key"));
    setText(_key.getString());
    if (_key.hasFlags(KeyValue.FLAG_KEY_FONT))
      setTypeface(Theme.getSpecialKeyFont(context));
  }

  public void onClick(View v)
  {
    Config config = Config.globalConfig();
    config.handler.handleKeyUp(_key, Pointers.Modifiers.EMPTY);
  }
}
