package juloo.keyboard2;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class Keyboard2 extends InputMethodService
{
	public static final String		TAG = "Keyboard_2.0";

	private KeyboardData	_keyboardData;
	private Keyboard2View	_inputView;

	@Override
	public void				onCreate()
	{
		super.onCreate();
		_keyboardData = new KeyboardData(getResources().getXml(R.xml.azerty));
	}

	@Override
	public View				onCreateInputView()
	{
		_inputView = (Keyboard2View)getLayoutInflater().inflate(R.layout.input, null);
		_inputView.setKeyboard(this, _keyboardData);
		return (_inputView);
	}

	public static void		log(String str)
	{
		Log.d(TAG, str);
	}
}
