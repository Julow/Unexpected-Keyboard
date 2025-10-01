package juloo.keyboard2;

import static juloo.keyboard2.Logs.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.v1.InlineSuggestionUi;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.inline.InlinePresentationSpec;

import java.util.Collections;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class InlineAutofill {
  private HorizontalScrollView _autofillContainer;
  private LinearLayout _autofillSuggestions;

  public void onInflate(View keyboardViewParent)
  {
    _autofillContainer = keyboardViewParent.findViewById(R.id.autofill_container);
    _autofillSuggestions = keyboardViewParent.findViewById(R.id.autofill_suggestions);
    _autofillContainer.setVisibility(View.GONE);
  }

  @Nullable
  public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(@NonNull Bundle uiExtras)
  {
    Size smallestSize = new Size(0, 0);
    Size biggestSize = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);

    UiVersions.StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();

    InlineSuggestionUi.Style style = InlineSuggestionUi.newStyleBuilder().build();
    stylesBuilder.addStyle(style);

    Bundle stylesBundle = stylesBuilder.build();
    InlinePresentationSpec spec =
            new InlinePresentationSpec.Builder(smallestSize, biggestSize)
                    .setStyle(stylesBundle)
                    .build();

    return new InlineSuggestionsRequest.Builder(Collections.singletonList(spec)).build();
  }

  public boolean onInlineSuggestionsResponse(Context context, @NonNull InlineSuggestionsResponse response)
  {
    List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();

    float height =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
    Size autofillSize = new Size(ViewGroup.LayoutParams.WRAP_CONTENT, ((int) height));

    _autofillSuggestions.removeAllViews();
    if (!inlineSuggestions.isEmpty()) _autofillContainer.setVisibility(View.VISIBLE);

    for (InlineSuggestion inlineSuggestion : inlineSuggestions)
    {
      try
      {
        inlineSuggestion.inflate(
                context,
                autofillSize,
                context.getMainExecutor(),
                _autofillSuggestions::addView);
      }
      catch (Exception e)
      {
        Log.e(TAG, "onInlineSuggestionsResponse - inlineSuggestion.infLate - " + e);
      }
    }
    return true;
  }
}