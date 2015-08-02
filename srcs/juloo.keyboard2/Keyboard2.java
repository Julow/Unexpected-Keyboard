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

	public void				handleKeyUp(KeyValue key, int flags)
	{
		int			eventCode = key.getEventCode();

		if (getCurrentInputConnection() == null)
			return ; // TODO wait a little before give up
		switch (eventCode)
		{
		case KeyValue.EVENT_NONE:
			sendKeyChar(key.getChar((flags & KeyValue.FLAG_SHIFT) != 0));
			break ;
		case KeyValue.EVENT_DELETE:
			getCurrentInputConnection().deleteSurroundingText(0, 1);
			break ;
		case KeyValue.EVENT_BACKSPACE:
			getCurrentInputConnection().deleteSurroundingText(1, 0);
			break ;
		default:
			getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, eventCode));
			getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, eventCode));
			break ;
		}
	}

	public static void		log(String str)
	{
		Log.d(TAG, str);
	}
}
