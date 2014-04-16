/**
 * Copyright 2013 Aalto university, see AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Collection;

import fi.aalto.legroup.achso.R;
import fi.aalto.legroup.achso.database.SemanticVideo;
import fi.aalto.legroup.achso.database.SerializableToDB;
import fi.aalto.legroup.achso.database.VideoDBHelper;

import static fi.aalto.legroup.achso.util.App.appendLog;

public class Dialog {
    private Dialog() {
    }

    private static void hideKeyboardAutomatically(final Context ctx, final View input) {
        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private static String getTextFromTextView(final TextView view) {
        String text = null;
        CharSequence cs = view.getText();
        String inputtext = "";
        if (cs != null) {
            inputtext = cs.toString();
        }
        if (!inputtext.equals("")) {
            text = inputtext;
        }
        return text;
    }

    private static void setInitialValueOfEditText(final EditText textView, final String initialValue) {
        textView.setSingleLine(true);
        if (initialValue != null) {
            textView.setText(initialValue);
            textView.setSelection(initialValue.length(), initialValue.length());
        }
    }

    private static DialogInterface.OnClickListener mGetTextSetterOnClick(final Context ctx, final EditText input,
                                                                         final SerializableToDB dbObject, final TextSettable ts,
                                                                         final DialogInterface.OnClickListener additionalOkListener) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hideKeyboardAutomatically(ctx, input);
                String new_value = getTextFromTextView(input);
                appendLog(String.format("Changed text in TextView %s to: %s", input.toString(), new_value));
                ts.setText(new_value);

                VideoDBHelper dbh = new VideoDBHelper(ctx);
                dbh.update(dbObject);
                dbh.close();

                additionalOkListener.onClick(dialog, which);

                dialog.dismiss();
            }
        };
    }

    public static AlertDialog getTextSetterDialog(final Context ctx, final SerializableToDB dbObject, String initialValue,
                                                  final DialogInterface.OnClickListener additionalOkListener, final TextSettable ts) {
        final EditText input = new EditText(ctx);
        setInitialValueOfEditText(input, initialValue);
        final DialogInterface.OnClickListener mClickListener = mGetTextSetterOnClick(ctx, input, dbObject, ts, additionalOkListener);
        final AlertDialog ret = new AlertDialog.Builder(ctx)
                .setView(input)
                .setPositiveButton(R.string.ok, mClickListener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        hideKeyboardAutomatically(ctx, input);
                        dialog.cancel();
                    }
                }).create();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mClickListener.onClick(ret, DialogInterface.BUTTON_POSITIVE);
                    return true;
                }
                return false;
            }
        });
        return ret;
    }

    public static AlertDialog getGenreDialog(Context ctx, DialogInterface.OnClickListener
            onGenreClick, DialogInterface.OnCancelListener onGenreCancel ) {
        Collection<String> values = SemanticVideo.genreStrings.values();
        CharSequence[] cs = values.toArray(new CharSequence[values.size()]);
        return new AlertDialog.Builder(ctx)
                .setTitle(R.string.select_genre)
                .setItems(cs, onGenreClick)
                .setOnCancelListener(onGenreCancel)
                .create();
    }
}
