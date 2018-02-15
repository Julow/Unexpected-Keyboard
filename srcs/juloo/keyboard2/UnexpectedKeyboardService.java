package juloo.keyboard2;

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import juloo.javacaml.Value;
import juloo.javacaml.Caml;

public class UnexpectedKeyboardService extends InputMethodService
{
	static
	{
		System.loadLibrary("unexpected-keyboard");
		Caml.startup();
	}

	Value			obj;

	@Override
	public void		onCreate()
	{
		super.onCreate();
		Caml.function(Caml.getCallback("unexpectedKeyboardService"));
		Caml.argObject(this);
		obj = Caml.callValue();
	}

	static final int m_id_onInitializeInterface = Caml.hashVariant("onInitializeInterface");
	@Override
	public void		onInitializeInterface()
	{
		Caml.method(obj, m_id_onInitializeInterface);
		Caml.callUnit();
	}

	static final int m_id_onBindInput = Caml.hashVariant("onBindInput");
	@Override
	public void		onBindInput()
	{
		Caml.method(obj, m_id_onBindInput);
		Caml.callUnit();
	}

	static final int m_id_onCreateInputView = Caml.hashVariant("onCreateInputView");
	@Override
	public View		onCreateInputView()
	{
		Caml.method(obj, m_id_onCreateInputView);
		return (View)Caml.callObject();
	}

	static final int m_id_onCreateCandidatesView = Caml.hashVariant("onCreateCandidatesView");
	@Override
	public View		onCreateCandidatesView()
	{
		Caml.method(obj, m_id_onCreateCandidatesView);
		return (View)Caml.callObject();
	}

	static final int m_id_onCreateExtractTextView = Caml.hashVariant("onCreateExtractTextView");
	@Override
	public View		onCreateExtractTextView()
	{
		Caml.method(obj, m_id_onCreateExtractTextView);
		return (View)Caml.callObject();
	}


	static final int m_id_onStartInput = Caml.hashVariant("onStartInput");
	@Override
	public void		onStartInput(EditorInfo attribute, boolean restarting)
	{
		Caml.method(obj, m_id_onStartInput);
		Caml.argObject(attribute);
		Caml.argBool(restarting);
		Caml.callUnit();
	}
}
