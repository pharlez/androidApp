package gr.unfold.android.tsibato.wsclient;

import gr.unfold.android.tsibato.AppConfig;
import gr.unfold.android.tsibato.async.AbstractAsyncTask;
import gr.unfold.android.tsibato.data.Category;

import java.util.ArrayList;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.util.Log;

public class GetCategoriesTask extends AbstractAsyncTask<SoapObject, ArrayList<Category>> {
	
	private static final String TAG = "GetCategoriesTask";
	
	private static final String WSDL_URL = "http://www.tsibato.gr/ws/iphone.asmx?WSDL";
    private static final String WS_NAMESPACE = "http://tempuri.org/";
    private static final String WS_METHOD_NAME = "GetCategories";
    
    public GetCategoriesTask() {
    	
    }
    
    public static SoapObject createRequest() {
    	return new SoapObject(WS_NAMESPACE, WS_METHOD_NAME);
    }
    
    @Override
    protected ArrayList<Category> executeTask(SoapObject parameter) throws Exception {
    	// 1. Create SOAP Envelope
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        // 2. Set the request parameters
        envelope.setOutputSoapObject(parameter);
        envelope.dotNet = true;

        // 3. Create a HTTP Transport object to send the web service request
        HttpTransportSE httpTransport = new HttpTransportSE(WSDL_URL);
        if (AppConfig.DEBUG) {
        	httpTransport.debug = true; // allows capture of raw request/response in Logcat
        }

        // 4. Make the web service invocation
        httpTransport.call(WS_NAMESPACE + WS_METHOD_NAME, envelope);

        if (AppConfig.DEBUG) {
        	Log.d(TAG, "HTTP REQUEST:\n" + httpTransport.requestDump);
        	Log.d(TAG, "HTTP RESPONSE:\n" + httpTransport.responseDump);
        }
        
        ArrayList<Category> result = new ArrayList<Category>();
        if (envelope.bodyIn instanceof SoapObject) { // SoapObject = SUCCESS
            SoapObject soapObject = (SoapObject) envelope.bodyIn;
            result = parseSOAPResponse(soapObject);
        } else if (envelope.bodyIn instanceof SoapFault) { // SoapFault = FAILURE
            SoapFault soapFault = (SoapFault) envelope.bodyIn;
            throw new Exception(soapFault.getMessage());
        }

        return result;
    }
    
    private ArrayList<Category> parseSOAPResponse(SoapObject response) {
    	ArrayList<Category> result = new ArrayList<Category>();
    	SoapObject categoriesResult = (SoapObject) response.getProperty("GetCategoriesResult");
    	if (categoriesResult != null) {
    		for (int i = 0; i < categoriesResult.getPropertyCount(); i++) {
    			SoapObject categoryDescription = (SoapObject) categoriesResult.getProperty(i);
    			int categoryId = Integer.parseInt(categoryDescription.getPrimitivePropertySafelyAsString("Id"));
    			String categoryName = categoryDescription.getPrimitivePropertySafelyAsString("Name");
    			
    			result.add(new Category(categoryId, categoryName));
    		}
    	}
    	
    	return result;
    }
}
