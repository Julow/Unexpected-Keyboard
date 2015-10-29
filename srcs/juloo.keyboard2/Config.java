package juloo.keyboard2;

import android.content.res.Resources;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;

class Config
{
	private Keyboard2			_context;


	public final long			previewDismissTimeout;
	public final int			previewBottomMargin;
	public final float			marginTop;
	public final float			keyPadding;
	public final float			keyBgPadding;
	public final float			keyRound;

	public boolean				previewEnabled;
	public float				subValueDist;
	public boolean				vibrateEnabled;
	public long					vibrateDuration;
	public long					longPressTimeout;
	public long					longPressInterval;
	public float				marginBottom;
	public float				keyHeight;
	public float				horizontalMargin;

	public Config(Keyboard2 context)
	{
		Resources			res = context.getResources();

		_context = context;
		// static values
		previewDismissTimeout = 150;
		previewBottomMargin = (int)res.getDimension(R.dimen.preview_margin);
		marginTop = res.getDimension(R.dimen.margin_top);
		keyPadding = res.getDimension(R.dimen.key_padding);
		keyBgPadding = res.getDimension(R.dimen.key_bg_padding);
		keyRound = res.getDimension(R.dimen.key_round);
		// default values
		previewEnabled = false;
		subValueDist = 10f;
		vibrateEnabled = true;
		vibrateDuration = 20;
		longPressTimeout = 600;
		longPressInterval = 65;
		marginBottom = res.getDimension(R.dimen.margin_bottom);
		keyHeight = res.getDimension(R.dimen.key_height);
		horizontalMargin = res.getDimension(R.dimen.horizontal_margin);
		// from prefs
		refresh();
	}

	/*
	** Reload prefs
	*/
	public void			refresh()
	{
		SharedPreferences	prefs = PreferenceManager.getDefaultSharedPreferences(_context);

		previewEnabled = prefs.getBoolean("preview_enabled", previewEnabled);
		subValueDist = prefs.getFloat("sub_value_dist", subValueDist);
		vibrateEnabled = prefs.getBoolean("vibrate_enabled", vibrateEnabled);
		vibrateDuration = prefs.getInt("vibrate_duration", (int)vibrateDuration);
		longPressTimeout = prefs.getInt("longpress_timeout", (int)longPressTimeout);
		longPressInterval = prefs.getInt("longpress_interval", (int)longPressInterval);
		marginBottom = getDipPref(prefs, "margin_bottom", marginBottom);
		keyHeight = getDipPref(prefs, "key_height", keyHeight);
		horizontalMargin = getDipPref(prefs, "horizontal_margin", horizontalMargin);
	}

	private float		getDipPref(SharedPreferences prefs, String pref_name, float def)
	{
		int					value = prefs.getInt(pref_name, -1);

		if (value < 0)
			return (def);
		return (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
			_context.getResources().getDisplayMetrics()));
	}
}
