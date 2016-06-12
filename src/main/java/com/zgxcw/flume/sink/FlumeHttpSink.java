/**
 * 
 */
package com.zgxcw.flume.sink;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventHelper;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zgxcw.flume.utils.JsonUtils;
import com.zgxcw.flume.utils.Utils;

/**
 * @author summer
 *
 */
public class FlumeHttpSink extends AbstractSink implements Configurable{
	
	private final static Logger logger = LoggerFactory.getLogger(FlumeHttpSink.class);
	

	private int     batchSize;
	private SinkCounter sinkCounter;
	private final static String DEFAULT_ENCODING = "UTF-8";
	private final static Charset UTF8_CHARSET = Charset.forName(DEFAULT_ENCODING);
	private Charset charset;
	private HttpClientGenerator httpclient;
	private Map<String,Integer> eventMaps;
	private ScheduledExecutorService executor;
	private EventSenderRunner runner;
	 
	public Status process() throws EventDeliveryException {
		
		Status status = Status.READY;
		Channel channel = getChannel();
		Event event = null;
		Transaction transaction = channel.getTransaction();
		try{
			transaction.begin();
			List<Event> events = Lists.newLinkedList();
			for(int i=0;i<getBatchSize();i++){
				event = channel.take();
		        if (event == null) {
		          break;
		        }
		        events.add(event);
			}
			
			int size = events.size();
			int batchSize = getBatchSize();
			if(size == 0){
				sinkCounter.incrementBatchEmptyCount();
				status = Status.BACKOFF;
			}else{
				if(size < batchSize){
				    sinkCounter.incrementBatchUnderflowCount();
				} else {
				    sinkCounter.incrementBatchCompleteCount(); 
				}
				sinkCounter.addToEventDrainAttemptCount(size);
				
				String hostName;
				String logTime;
				String key;
				
				for(int i = 0;i < size ;i++){
					Event e = events.get(i);
					if(logger.isDebugEnabled()){
						logger.debug("Event:" + EventHelper.dumpEvent(e));
					}
					Map<String,Object> eventMap = toMap(e);
					if(eventMap.containsKey(FlumeHttpSinkContants.EVENT_HOSTNAME_KEY)){
						if(eventMap.containsKey(FlumeHttpSinkContants.EVENT_LOGTIME_KEY)){
							logTime = eventMap.get(FlumeHttpSinkContants.EVENT_LOGTIME_KEY).toString();
							hostName = eventMap.get(FlumeHttpSinkContants.EVENT_HOSTNAME_KEY).toString();
							key = formatKey(logTime,hostName);
							if(key != null)
							synchronized(eventMaps){
								if(eventMaps.containsKey(key)){
									eventMaps.put(key, eventMaps.get(key)+1);
								}else{
									eventMaps.put(key, 1);
								}
							}
						}
						
					}
					
					
				}
			}
			
			transaction.commit();
		}catch(Exception ex){
			transaction.rollback();
			logger.error("transaction will be rollback.", ex);
			throw new EventDeliveryException("Failed to monitor file event",ex);
		}finally{
			transaction.close();
		}
		return status;
	}

	
	
	private int getBatchSize() {
		
		return batchSize;
	}


	private String formatKey(String logtime,String hostName){
		
		return Utils.toDate0(logtime)+"$$"+hostName;
	}
	

	/**
	 *  source serverName hbase:servername storm:storm ���� servername
	 *  resource  ��: hbase 1 storm 2
	 *  model model json message�� MODEL_TAG=name
	 *   body json message <����� ,ֵ>
	 */
	
	/**
	 * 
	 * @param model
	 * @param hostName
	 * @param resource
	 * @param timestamp
	 * @param flowcount
	 * @return
	 */
	public String getSchema(String model,String hostName,String resource,String timestamp,int flowcount){
		return String.format(FlumeHttpSinkContants.DEFAULT_SCHEMA,
				new Object[]{ model,hostName,resource,timestamp,flowcount});
	}
	
	
	private Map<String,Object> toMap(Event event){
		
		byte[] body = event.getBody();
		if(body == null)
			return Maps.newHashMap();
		@SuppressWarnings("unchecked")
		Map<String,Object> map = JsonUtils.fromJson(new String(body,charset), Map.class);
		return map;
	}
	
	class EventSenderRunner implements Runnable{
		private HttpClientGenerator httpclient;
		
		public EventSenderRunner(HttpClientGenerator httpclient){
			this.httpclient = httpclient;
		}
		public void run() {
			
			String schema;
			String hostName;
			String timeStamp;
			int    flowCount;
			while(true){
				synchronized(eventMaps){
					Iterator<Map.Entry<String, Integer>> it = eventMaps.entrySet().iterator();
					for(;it.hasNext();){
						Map.Entry<String, Integer> e = it.next();
						flowCount = e.getValue();
						String[] tsAndHost = e.getKey().split("$$");
						if(tsAndHost.length == 2){
							timeStamp = tsAndHost[0];
							hostName =  tsAndHost[1];
							schema = getSchema(
										FlumeHttpSinkContants.PROTOCOL_HEADER_MODEL_WEBLOG,
										hostName,
										FlumeHttpSinkContants.PROTOCOL_HEADER_MODEL_WEBLOG,
										timeStamp,
										flowCount
									 );
							if(logger.isDebugEnabled()){
								logger.debug("{"
										+"httpReqPath:"+httpReqPath+"\n"
										+"message:"+schema+"\n"
										+"charset:"+charset.displayName()
										+ "}");
							}
							try {
								httpclient.sendEvent(httpReqPath,schema,charset);
							} catch (EventDeliveryException ex) {
								ex.printStackTrace();
							}
						}
						it.remove();
					}//for
				}//synchronized(eventMaps)
				
				break;
			}//while
			
		}
		
	}
	
	@Override
	public synchronized void start() {
		
		charset = UTF8_CHARSET;
		eventMaps = Maps.newHashMap();
		httpclient = HttpClientGenerator.Builder.builder().create();
		runner = new EventSenderRunner(httpclient);
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(runner, 200, 1000*60, TimeUnit.MILLISECONDS);
		sinkCounter.start();
		super.start();
		
		if(logger.isDebugEnabled()){
			
			logger.debug("{} started=>"
					+"http server params:["
					+"httpServerAddr:"+httpServerAddr+","
					+"httpServerPort:"+httpServerPort+","
					+"httpReqPath:"+httpReqPath+","
					+"httpSSL:"+httpSSL+","
					+"retryTimesWhenConnectedFail:"+retryTimesWhenConnectedFail
					+ "]",getName());
		}
		
	}



	@Override
	public synchronized void stop() {
		
		if(httpclient!=null)
			httpclient.close();
		executor.shutdown();
		sinkCounter.stop();
		if(logger.isDebugEnabled())
			logger.debug("Flume Http Sink {} stopped. Metrics: {}", getName(), sinkCounter);
		if(eventMaps!=null)
			eventMaps.clear();
		super.stop();
	}



	public void configure(Context context) {
		
		httpServerAddr = context.getString(FlumeHttpSinkContants.HTTP_SERVER_ADDR_KEY, 
				FlumeHttpSinkContants.DEFAULT_HTTP_SERVER_ADDR);
		Preconditions.checkArgument(httpServerAddr != null, "http server addr is illegal argument");
		httpServerPort = context.getInteger(FlumeHttpSinkContants.HTTP_SERVER_PORT_KEY, 
				FlumeHttpSinkContants.DEFAULT_HTTP_SERVER_PORT);
		
		Preconditions.checkArgument(httpServerPort >23, "http server port is illegal argument");
		httpSSL = context.getBoolean(FlumeHttpSinkContants.HTTP_SSL_ENABLE_KEY, false);
		retryTimesWhenConnectedFail = context.getInteger(FlumeHttpSinkContants.HTTP_CONNECT_FAIL_RETRY_TIMES_KEY,
				FlumeHttpSinkContants.DEFAULT_HTTP_CONNECT_FAIL_RETRY_TIMES);
		Preconditions.checkArgument(retryTimesWhenConnectedFail >-1, "retry times when connected fail is illegal argument");
		
		batchSize = context.getInteger(FlumeHttpSinkContants.BATCH_SIZE_KEY, FlumeHttpSinkContants.DEFAULT_BATCH_SIZE);
		Preconditions.checkArgument(batchSize >0, "batch size is illegal argument");
		
		httpReqPath = context.getString(FlumeHttpSinkContants.HTTP_SERVER_REQUEST_PATH_KEY, 
				FlumeHttpSinkContants.DEFAULT_HTTP_SERVER_REQUEST_PATH);
		Preconditions.checkArgument(httpReqPath != null, "http req path is illegal argument");
		
		if(!httpReqPath.startsWith("/"))
			httpReqPath = "/"+httpReqPath;
		httpReqPath = String.format(FlumeHttpSinkContants.HTTP_REQUEST_PATH,
				httpServerAddr,httpServerPort,httpReqPath);
		
		if(sinkCounter == null)
			sinkCounter = new SinkCounter(getName());
		
		
	}
	
	private String 	httpServerAddr;
	private int    	httpServerPort;
	private String 	httpReqPath;
	private Boolean httpSSL;
	private int    	retryTimesWhenConnectedFail;

	
}
