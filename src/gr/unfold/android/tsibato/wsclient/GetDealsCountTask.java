package gr.unfold.android.tsibato.wsclient;

import gr.unfold.android.tsibato.AppConfig;
import gr.unfold.android.tsibato.async.AsyncTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class GetDealsCountTask extends AsyncTask<String, String, String> {
	
	private final String TAG = getClass().getName();
	
	final static String SITE_DOMAIN = "http://www.tsibato.gr/";
	final static String SERVICE_PATH = "ws/xml.aspx?action=count";
	
	private AsyncTaskListener<String> taskListener;
    // Most recent exception (used to diagnose failures)
    private Exception mostRecentException;
	
	public GetDealsCountTask() {
		
	}
	
	public final void setTaskListener(AsyncTaskListener<String> taskListener) {
    	this.taskListener = taskListener;
    }
	
	/** Invoke the web service request */
    @Override
    protected final String doInBackground(String...parameters) {
        mostRecentException = null;
        String result = null;

        try {
            result = executeTask();
        } catch (Exception e) {
        	if (AppConfig.DEBUG) {
        		Log.e(TAG, "Failed to invoke the web service: ", e);
        	}
            mostRecentException = e;
        }

        return result;
    }
    
    protected String executeTask() throws Exception {
    	HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String uri = SITE_DOMAIN + SERVICE_PATH;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {

        if (taskListener != null) {
            if (result == null || mostRecentException != null) {
            	taskListener.onTaskFailed(mostRecentException);

            } else {
            	taskListener.onTaskCompleteSuccess(result);
            }
        }

        // clean up listeners since we are done with this task
        taskListener = null;
    }

}
