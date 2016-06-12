/**
 * 
 */
package com.zgxcw.flume.sink;

import org.junit.Test;

/**
 * @author summer
 *
 */
public class StringFormatTest {

	@Test
	public void schemaTest(){
		
		System.out.println( String.format(FlumeHttpSinkContants.DEFAULT_SCHEMA,
				new Object[]{ "weblog","localhost","weblog","201506121810",1}));
	}
	@Test
	public void httpRequestTest(){
		String httpReqPath = "metrics";
		if(!httpReqPath.startsWith("/"))
			httpReqPath = "/"+httpReqPath;
		System.out.println( String.format(FlumeHttpSinkContants.HTTP_REQUEST_PATH,
				"127.0.0.1",7080,httpReqPath));
	}
}
