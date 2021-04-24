package juloo.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;

/*
** SideBarPreference
** -
** Open a dialog showing a seekbar
** -
** xml attrs:
**   android:defaultValue		Default value (float)
**   min						min value (float)
**   max						max value (float)
** -
** Summary field allow to show the current value using %f or %s flag
*/
public class SlideBarPreference extends DialogPreference
	implements SeekBar.OnSeekBarChangeListener
{
	private static final int	STEPS = 100;

	private LinearLayout	_layout;
	private TextView		_textView;
	private SeekBar			_seekBar;

	private float			_min;
	private float			_max;
	private float			_value;

	private String			_initialSummary;

	public SlideBarPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		_initialSummary = getSummary().toString();
		_textView = new TextView(context);
		_textView.setPadding(48, 40, 48, 40);
		_seekBar = new SeekBar(context);
		_seekBar.setOnSeekBarChangeListener(this);
		_seekBar.setMax(STEPS);
		_min = float_of_string(attrs.getAttributeValue(null, "min"));
		_value = _min;
		_max = Math.max(1f, float_of_string(attrs.getAttributeValue(null, "max")));
		_layout = new LinearLayout(getContext());
		_layout.setOrientation(LinearLayout.VERTICAL);
		_layout.addView(_textView);
		_layout.addView(_seekBar);
	}

	@Override
	public void				onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		_value = Math.round(progress * (_max - _min)) / (float)STEPS + _min;
		updateText();
	}

	@Override
	public void				onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void				onStopTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	protected void			onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		if (restorePersistedValue)
		{
			_value = getPersistedFloat(_min);
		}
		else
		{
			_value = (Float)defaultValue;
			persistFloat(_value);
		}
		_seekBar.setProgress((int)((_value - _min) * STEPS / (_max - _min)));
		updateText();
	}

	@Override
	protected Object		onGetDefaultValue(TypedArray a, int index)
	{
		return (a.getFloat(index, _min));
	}

	@Override
	protected void			onDialogClosed(boolean positiveResult)
	{
		if (positiveResult)
			persistFloat(_value);
	}

	protected View			onCreateDialogView()
	{
		ViewGroup		parent = (ViewGroup)_layout.getParent();

		if (parent != null)
			parent.removeView(_layout);
		return (_layout);
	}

	private void			updateText()
	{
		String			f = String.format(_initialSummary, _value);

		_textView.setText(f);
		setSummary(f);
	}

	private static float	float_of_string(String str)
	{
		if (str == null)
			return (0f);
		return (Float.parseFloat(str));
	}
}
