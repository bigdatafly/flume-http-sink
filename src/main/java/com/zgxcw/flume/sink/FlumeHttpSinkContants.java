/**
 * 
 */
package com.zgxcw.flume.sink;

/**
 * @author summer
 *
 */
public class FlumeHttpSinkContants {

	public static final String HTTP_SERVER_ADDR_KEY = "http.server.addr";
	
	public static final String DEFAULT_HTTP_SERVER_ADDR = "localhost";
	
	public static final String HTTP_SERVER_PORT_KEY = "http.server.port";
	
	public static final int DEFAULT_HTTP_SERVER_PORT = 7080;
	
	public static final String HTTP_SERVER_REQUEST_PATH_KEY = "http.server.req.path";
	
	public static final String DEFAULT_HTTP_SERVER_REQUEST_PATH = "/metric";
	
	public static final boolean DEFAULT_HTTP_SSL_ENABLE = false;
	
	public static final String HTTP_SSL_ENABLE_KEY = "http.ssl";
	
	public static final String HTTP_CONNECT_FAIL_RETRY_TIMES_KEY = "http.connect.failretrytimes";
	
	public static final int DEFAULT_HTTP_CONNECT_FAIL_RETRY_TIMES = 10;
	
	public static final String BATCH_SIZE_KEY = "http.batchsize";
	
	public static final int   DEFAULT_BATCH_SIZE = 128;
	
	public static final String DEFAULT_SCHEMA = "{\"beans\":[{"
			+"\"name\":"+"\"%s\","
			+"\"source\":"+"\"%s\","
			+"\"resource\":"+"\"%s\","
			+"\"timestamp\":"+"\"%s\","
			+"\"monitorFlow\":"+"\"%d\""
			+ "}]"
			+ "}";
	public static final String DEFAULT_RESOURCE_TAG = "resource";
	
	public static final String HTTP_REQUEST_PATH = "http://%s:%d%s";
	
	public static final String EVENT_HOSTNAME_KEY = "ip";
	
	public static final String EVENT_WEBLOG_FLOW_KEY = "len";
	
	public static final String EVENT_LOGTIME_KEY = "logtime";
	
	public static final String PROTOCOL_HEADER_MODEL_WEBLOG="weblog";
	

	
}
