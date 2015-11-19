package io.github.xSocks.ui;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.github.xSocks.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class AppManagerActivityFragment extends Fragment {

    public AppManagerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_manager, container, false);
    }
}
