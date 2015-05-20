package io.github.xsocks.utils;

import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class MovementCheck extends LinkMovementMethod {

    @Override
    public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
        try {
            return super.onTouchEvent(widget, buffer, event);
        } catch (Exception ex) {
            return true;
        }
    }

}