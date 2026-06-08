package com.codex.abyssdeck;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    private GameView view;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        view = new GameView(this);
        setContentView(view);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (view != null) {
            view.saveNow();
        }
    }
}
