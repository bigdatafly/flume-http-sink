/**
 * 
 */
package com.zgxcw.flume.sink;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.EventDeliveryException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author summer
 *
 */
public class HttpClientGenerator {

	private final static Logger logger = LoggerFactory.getLogger(HttpClientGenerator.class);
	
	private PoolingHttpClientConnectionManager connectionManager;
	private CloseableHttpClient httpclient;
	private final static Charset DEFAULT_CHARSET = Charset.defaultCharset();
	
	protected HttpClientGenerator(){
		
	}
	
	protected CloseableHttpClient createHttpClient(){
		
		Registry<ConnectionSocketFactory> registry = 
				RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();
		connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setDefaultMaxPerRoute(100);
		connectionManager.setMaxTotal(100);
		return HttpClientBuilder
				.create()
				.setConnectionManager(connectionManager)
				.setDefaultSocketConfig(socketConfig())
				.build();
	}
	
	protected SocketConfig socketConfig(){
		return SocketConfig.custom()
				.setSoKeepAlive(true)
				.setTcpNoDelay(true)
				.build();
	}
	
	
	public enum METHOD{
		GET,
		POST
	}
	
	public void sendEvent(String url,String event) throws EventDeliveryException{
		
		try {
			sendEvent(url,event,DEFAULT_CHARSET);
		} catch (EventDeliveryException e) {
			
			throw new EventDeliveryException(e);
		}
	}
	/**
	 * 
	 * @param url
	 * @param event
	 * @param charset
	 * @throws EventDeliveryException
	 */
	public void sendEvent(String url,String event,Charset charset) throws EventDeliveryException{
		
		try {
			sendEvent(httpclient,url,METHOD.POST,event,charset);
		} catch (IOException e) {
			
			throw new EventDeliveryException(e);
		}
	}
	
	
	protected void sendEvent(CloseableHttpClient httpclient,String url,METHOD method,String event,Charset charset) throws IOException{
		
		if(charset == null)
			charset = DEFAULT_CHARSET;
		if(StringUtils.isEmpty(event))
			event = "";
		if(StringUtils.isEmpty(url)){
			throw new IOException("url ("+url + ") must not be null");
		}
		HttpUriRequest request;
		if(method == METHOD.GET){
			request = new HttpGet(url);
			
		}else{
			request = new HttpPost(url);
			byte[] byteBuf = event.getBytes(charset);
			HttpEntity entity = EntityBuilder.create().setBinary(byteBuf).build();
			((HttpPost)request).setEntity(entity);
			
		}
		try{
			CloseableHttpResponse response = httpclient.execute(request); 
			StringBuffer result = new StringBuffer();
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode == HttpStatus.SC_OK){
				HttpEntity entity = response.getEntity();
				//encoding= encodingName = DEFAULT_ENCODING ;
				if(entity!=null){
					result.append(getContent(entity,charset));
				}
	
			}else if(statusCode == HttpStatus.SC_BAD_REQUEST){ //400
				if(logger.isDebugEnabled())
					logger.debug(" {error} Http 400 Error, Maybe Jmx qry parameter IllegalArgumentException");
				throw new IOException("Http 400 Error, Maybe parameters url is illegal");
			}else{ //can not be found will return code SC_NOT_FOUND
				if(logger.isDebugEnabled())
					logger.debug("{error:404} "+url+"page not found, Maybe url is illegal");
				throw new IOException(url + " page not found");
			}
		} catch (IOException e) {
			if(logger.isDebugEnabled()){
				logger.debug("{exception}",e);
			}
			throw e;
		}
	}
	
	private String getContent(HttpEntity entity,Charset charset){
		try{
			return EntityUtils.toString(entity, charset);
		}catch(Exception ex){
			return "";
		}
		/*
		 * 
		int readBytes = 0;
		InputStream in = entity.getContent();
		if(in!=null){
			br = new BufferedReader(new InputStreamReader(in,charset));
			char[] bytebuf ;
			
			while(true){
				bytebuf = new char[BUFF_SIZE];
				readBytes = br.read(bytebuf) ;
				if(readBytes == -1)
					break;
				result.append(bytebuf);
			}
		}
		*/
	}
	
	public void close(){
		try {
			httpclient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class Builder{
		
		private Builder(){}
		
		public static Builder builder(){
			return new Builder();
		}
		
		public HttpClientGenerator create(){
			
			HttpClientGenerator generator = new HttpClientGenerator();
			generator.httpclient = generator.createHttpClient();
			return generator;
		}
	}
}
