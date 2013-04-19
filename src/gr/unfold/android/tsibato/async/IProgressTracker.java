package gr.unfold.android.tsibato.async;

import android.os.AsyncTask;

/**
 *	Callback interface to monitor life cycle of an {@link AsyncTask}
 */
public interface IProgressTracker {

    void onStartProgress();

    void onStopProgress();
}
