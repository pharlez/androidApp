package gr.unfold.android.tsibato.wsclient;

import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.util.Log;

import gr.unfold.android.tsibato.BuildConfig;
import gr.unfold.android.tsibato.async.AbstractAsyncTask;
import gr.unfold.android.tsibato.data.Deal;

public class GetDealsTask extends AbstractAsyncTask<SoapObject, ArrayList<Deal>> {
	
	private static final String TAG = "GetDealsTask";
	
	private static final String WSDL_URL = "http://www.tsibato.gr/ws/iphone.asmx?WSDL";
    private static final String WS_NAMESPACE = "http://tempuri.org/";
    private static final String WS_METHOD_NAME = "GetDealsAdvanced";
    
    public GetDealsTask() {
    
    }
    
    public static SoapObject createRequest(int page, int locationId, ArrayList<Integer> categories) {
    	SoapObject request = new SoapObject(WS_NAMESPACE, WS_METHOD_NAME);
    	
    	PropertyInfo locationProperty = new PropertyInfo();
    	locationProperty.setNamespace(WS_NAMESPACE); // to ensure that the element-name is prefixed with the namespace
    	locationProperty.setName("locationId");
    	locationProperty.setValue(locationId);
        
        request.addProperty(locationProperty);

        PropertyInfo pageProperty = new PropertyInfo();
        pageProperty.setNamespace(WS_NAMESPACE); // to ensure that the element-name is prefixed with the namespace
        pageProperty.setName("page");
        pageProperty.setValue(page);
        
        request.addProperty(pageProperty);
        
        SoapObject soapCategories = new SoapObject(WS_NAMESPACE, "categoryIds");
        for (Integer i : categories){
        	soapCategories.addProperty("int", i);
        }
        request.addSoapObject(soapCategories);
        
        PropertyInfo rankProperty = new PropertyInfo();
        rankProperty.setNamespace(WS_NAMESPACE); // to ensure that the element-name is prefixed with the namespace
        rankProperty.setName("rank");
        rankProperty.setValue(1);
        
        request.addProperty(rankProperty);
        
        List<Integer> providers =  new ArrayList<Integer>();

        SoapObject soapProviders = new SoapObject(WS_NAMESPACE, "providerIds");
        for (Integer i : providers){
        	soapProviders.addProperty("int", i);
        }
        request.addSoapObject(soapProviders);        
        
        if (BuildConfig.DEBUG) {
        	Log.d(TAG, "Feching deals page: " + page + "...");
        }
        
        return request;
    }
    
    @Override
    protected ArrayList<Deal> executeTask(SoapObject parameter) throws Exception {
    	// 1. Create SOAP Envelope
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        // 2. Set the request parameters
        envelope.setOutputSoapObject(parameter);
        envelope.dotNet = true;

        // 3. Create a HTTP Transport object to send the web service request
        HttpTransportSE httpTransport = new HttpTransportSE(WSDL_URL);
        if (BuildConfig.DEBUG) {
        	httpTransport.debug = true; // allows capture of raw request/response in Logcat
        }
        
        // 4. Make the web service invocation
        httpTransport.call(WS_NAMESPACE + WS_METHOD_NAME, envelope);
        
        if (BuildConfig.DEBUG) {
        	Log.d(TAG, "HTTP REQUEST:\n" + httpTransport.requestDump);
        	Log.d(TAG, "HTTP RESPONSE:\n" + httpTransport.responseDump);
        }
        
        ArrayList<Deal> result = new ArrayList<Deal>();
        if (envelope.bodyIn instanceof SoapObject) { // SoapObject = SUCCESS
            SoapObject soapObject = (SoapObject) envelope.bodyIn;
            result = parseSOAPResponse(soapObject);
        } else if (envelope.bodyIn instanceof SoapFault) { // SoapFault = FAILURE
            SoapFault soapFault = (SoapFault) envelope.bodyIn;
            throw new Exception(soapFault.getMessage());
        }

        return result;
    }
    
    private ArrayList<Deal> parseSOAPResponse(SoapObject response) {
    	ArrayList<Deal> result = new ArrayList<Deal>();
    	SoapObject dealsResult = (SoapObject) response.getProperty("GetDealsAdvancedResult");
    	if (dealsResult != null) {
    		for (int i = 0; i < dealsResult.getPropertyCount(); i++) {
    			SoapObject dealItem = (SoapObject) dealsResult.getProperty(i);
    			
    			int dealId = Integer.parseInt(dealItem.getPrimitivePropertySafelyAsString("Id"));
    			String dealTitle = dealItem.getPrimitivePropertySafelyAsString("Title");
    			String dealThumbnail = dealItem.getPrimitivePropertySafelyAsString("Thumbnail");
    			String providerLogo = dealItem.getPrimitivePropertySafelyAsString("ProviderLogo");
    			int dealPurchases = Integer.parseInt(dealItem.getPrimitivePropertySafelyAsString("Purchases"));
    			double dealPrice = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("Price"));
    			double dealValue = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("Value"));
    			double dealDiscount = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("DiscountPercent"));
    			double dealLong = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("Lon"));
    			double dealLat = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("Lat"));
    			double dealMapZoom = Double.parseDouble(dealItem.getPrimitivePropertySafelyAsString("MapZoom"));
    			
    			result.add(new Deal(dealId, dealTitle, dealThumbnail, providerLogo, dealPurchases, 
    					BigDecimal.valueOf(dealPrice).setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(dealValue).setScale(2, RoundingMode.HALF_UP), 
    					BigDecimal.valueOf(dealDiscount).setScale(0, RoundingMode.HALF_UP), dealLong, dealLat, dealMapZoom));
    		}
    	}
    	
    	if (BuildConfig.DEBUG) {
    		Log.d(TAG, "Loaded " + result.size() + "results!");
    	}
    	
    	return result;
    }

}
