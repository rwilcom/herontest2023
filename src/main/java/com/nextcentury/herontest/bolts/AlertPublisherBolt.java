package com.nextcentury.herontest.bolts;

import com.nextcentury.SpringScanner;
import com.nextcentury.herontest.HeronTestTopology;
import com.nextcentury.herontest.HeronTestTupleSchema;
import com.nextcentury.herontest.dto.Activity;
import com.nextcentury.herontest.dto.ActivityAlert;
import com.nextcentury.herontest.dto.Alert;
import com.nextcentury.utils.PubSubConfig;
import com.nextcentury.utils.PubSubService;
import com.twitter.heron.api.metric.GlobalMetrics;

import com.twitter.heron.api.bolt.BaseRichBolt;
import com.twitter.heron.api.bolt.OutputCollector;
import com.twitter.heron.api.topology.OutputFieldsDeclarer;
import com.twitter.heron.api.topology.TopologyContext;

import com.twitter.heron.api.tuple.Tuple;
import java.util.Date;

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 
 * 
 * 
 * 
 * 
 */
public class AlertPublisherBolt extends BaseRichBolt {

    private static final long serialVersionUID = -1L;
    private OutputCollector collector;
    
    PubSubConfig psConfig;
    PubSubService psService;    
    
    public static final String ALERT_PUBLISHER_NODE = "AlertPublisherBolt";
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map<String, Object> map, TopologyContext tc, OutputCollector oc) {
        collector = oc;
        
      try{
            //spring loaded beans
            SpringScanner.initializeSpring();
            psConfig = SpringScanner.getBean(PubSubConfig.class);
            psService = SpringScanner.getBean(PubSubService.class);

            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.INFO,
                    "Topology auto dependency injection initialized.\n");

           collector = oc;

            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.INFO,
                    "Connecting to PubSub:"+psConfig.getServerUri()+"\n");

            psService.startPublishing();
            
        }catch(Throwable any){
            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.SEVERE,
                    "Failed to inintialize PubSub: "+any.toString()+"\n");
        }               
    }
    
    @Override
    public void cleanup() {
        
        psService.endPublishing();
        
        super.cleanup(); //To change body of generated methods, choose Tools | Templates.        
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
         //nothing ot declare - not forwarding on to other bolts
    }

    @Override
    public void execute(Tuple tuple) {
        
        //multiple streams input
        Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.INFO,
                "execute - input stream name: "+tuple.getSourceStreamId()+"\n");        
        
        String payloadClass = tuple.getStringByField(HeronTestTupleSchema.SCHEMA_PAYLOADCLASS);        
        if(!payloadClass.equalsIgnoreCase(Alert.class.getName())){
            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.SEVERE,
                "execute - unexpected class in payload: "+payloadClass+"\n");
            collector.fail(tuple);
            return;
        }

        //proving rehydration here from bytes
        Alert alert;
        byte[] payloadAsBytes = tuple.getBinaryByField(HeronTestTupleSchema.SCHEMA_PAYLOAD_AS_BYTES);
        try{
            alert = Alert.getFromBytes(payloadAsBytes);
            //Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.INFO,
            //    "execute - rehydrated payload: "+alert.toString()+"\n");
        }catch( Exception any ){
            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.SEVERE,
                "execute - rehydration failed: "+payloadClass+"\n");
            collector.fail(tuple);
            return;
        }
            
        GlobalMetrics.incr("publishing_alert");

        //send alert to common pub/sub queue (Kafka?)
        publishAlert( alert );

        Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.INFO,
            "execute - alert firing  : "+alert.toString()+"\n");                
        
        //we are DONE with the original
        //tuple since we've processed the alert
        // -- end of stream
        collector.ack(tuple);      
    }

    
    /**
     * 
     * @param alert
     */
    private void publishAlert( Alert alert ){
        
        
        try {
            psService.publish( "alerts", alert.toStringTuple() );
        }catch(Exception any){
            Logger.getLogger(AlertPublisherBolt.class.getName()).log(Level.WARNING,
                "problem publishing alert "+alert.eventUuid+": "+any.toString()+"\n");
        }
    }
    
}        

