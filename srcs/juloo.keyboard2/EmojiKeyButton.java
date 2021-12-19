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
    setText(_key.symbol);
    if ((_key.flags & KeyValue.FLAG_KEY_FONT) != 0)
      setTypeface(((Keyboard2)context).getSpecialKeyFont());
  }

  public void onClick(View v)
  {
    Keyboard2 main = (Keyboard2)getContext();

    main.handleKeyUp(_key, 0);
  }
}
