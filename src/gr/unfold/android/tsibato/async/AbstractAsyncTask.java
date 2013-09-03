package gr.unfold.android.tsibato.async;

import gr.unfold.android.tsibato.AppConfig;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public abstract class AbstractAsyncTask<T, R> extends AsyncTask<T, Void, R> {
	
	private final String TAG = getClass().getName();
	
	private AsyncTaskListener<R> taskListener;
    private IProgressTracker progressTracker;
    // Most recent exception (used to diagnose failures)
    private Exception mostRecentException;
    
    public AbstractAsyncTask() {
    	
    }
    
    public final void setTaskListener(AsyncTaskListener<R> taskListener) {
    	this.taskListener = taskListener;
    }
    
    public final void setProgressTracker(IProgressTracker progressTracker) {
    	if (progressTracker != null) {
    		this.progressTracker = progressTracker;
    	}
    }
    
    @Override
    protected final void onPreExecute() {
        if (progressTracker != null) {
            this.progressTracker.onStartProgress();
        }
    }
    
    /** Invoke the web service request */
    @Override
    protected final R doInBackground(T... parameters) {
        mostRecentException = null;
        R result = null;

        try {
            result = executeTask(parameters[0]);
        } catch (Exception e) {
        	if (AppConfig.DEBUG) {
        		Log.e(TAG, "Failed to invoke the web service: ", e);
        	}
            mostRecentException = e;
        }

        return result;
    }
    
    /** Inherit this to insert specific task logic */
    protected abstract R executeTask(T parameter) throws Exception;
    
    /** @param result to be sent back to the observer (typically an {@link Activity} running on the UI Thread). This can be <code>null</code> if
     * an error occurs while attempting to invoke the web service (e.g. web service was unreachable, or network I/O issue etc.) */
    @Override
    protected final void onPostExecute(R result) {
        if (progressTracker != null) {
            progressTracker.onStopProgress();
        }

        if (taskListener != null) {
            if (result == null || mostRecentException != null) {
            	taskListener.onTaskFailed(mostRecentException);

            } else {
            	taskListener.onTaskCompleteSuccess(result);
            }
        }

        // clean up listeners since we are done with this task
        progressTracker = null;
        taskListener = null;
    }
}
