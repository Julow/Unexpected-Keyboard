package juloo.keyboard2;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;

public class Keyboard2 extends InputMethodService
{
	public static final String		TAG = "Keyboard_2.0";

	private Keyboard2View	_inputView;

	@Override
	public View				onCreateInputView()
	{
		_inputView = (Keyboard2View)getLayoutInflater().inflate(R.layout.input, null);
		_inputView.loadKeyboard(R.xml.azerty);
		return (_inputView);
	}

	public static void		log(String str)
	{
		Log.d(TAG, str);
	}
}
