/**
 * 
 */
package com.zgxcw.flume.sink;

import java.nio.charset.Charset;

import org.apache.flume.EventDeliveryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author summer
 *
 */
public class FlumeHttpSinkTest {

	private HttpClientGenerator httpclient;
	private String httpReqPath ;
	private String event;
	private Charset charset = Charset.forName("UTF-8");
	private String server = "172.27.101.171";
	@Before
	public void createHttpClient(){
		httpReqPath = "/metrics";
		if(!httpReqPath.startsWith("/"))
			httpReqPath = "/"+httpReqPath;
		
		httpReqPath = String.format(FlumeHttpSinkContants.HTTP_REQUEST_PATH,
				server,7080,httpReqPath);
		
		event =  String.format(FlumeHttpSinkContants.DEFAULT_SCHEMA,
				new Object[]{ "weblog","slave01","weblog",10342342});
		
		httpclient = HttpClientGenerator.Builder.builder().create();
	}
	@Test
	public void sendEventTest(){
		try {
			httpclient.sendEvent(httpReqPath, event, charset);
		} catch (EventDeliveryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@After
	public void close(){
		httpclient.close();
	}
}
