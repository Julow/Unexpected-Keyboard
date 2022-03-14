package juloo.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
/*
 ** ColorPreference 
 ** -
 ** Open a dialog showing a seekbar
 ** -
 ** xml attrs:
 **   android:defaultValue  Default value (String)
 ** -
 ** Summary field allow to show the current value using %s flag
 */
public class ColorPreference extends DialogPreference
    implements TextWatcher
{
  private LinearLayout _layout;
  private TextView _textView;
  private EditText _editText;

  private String _value;
  private String _initialSummary;
  private int _pos;

  public ColorPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _initialSummary = getSummary().toString();
    _textView = new TextView(context);
    _textView.setPadding(48, 40, 48, 40);
    _editText = new EditText(context);
    _editText.addTextChangedListener(this);
    _layout = new LinearLayout(getContext());
    _layout.setOrientation(LinearLayout.VERTICAL);
    _layout.addView(_textView);
    _layout.addView(_editText);
  }


  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
  public void onTextChanged(CharSequence s, int start, int before, int count) {}

  public void afterTextChanged(Editable s) {
    _value = "#" + _editText.getText().toString().replaceAll("[^0-9a-fA-F]", "");
    if (_value.length() >= 9)
      _value = _value.substring(0, 9);
    else
      _value = (_value + "000000").substring(0, 7);
    
    updateText();
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
  {
    String value;

    if (restorePersistedValue)
    {
      value = getPersistedString("");
    }
    else
    {
      value = (String)defaultValue;
      persistString(value);
    }
    _editText.setText(value);
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index)
  {
    return (a.getString(index));
  }

  @Override
  protected void onDialogClosed(boolean positiveResult)
  {
    if (positiveResult)
      persistString(_value);
  }

  protected View onCreateDialogView()
  {
    ViewGroup parent = (ViewGroup)_layout.getParent();

    if (parent != null)
      parent.removeView(_layout);
    return (_layout);
  }

  private void updateText()
  {
    String f = String.format(_initialSummary, _value);

    _textView.setText(f);
    setSummary(f);
  }
}
