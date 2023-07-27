package com.nextcentury.herontest;

import com.twitter.heron.api.topology.TopologyBuilder;
import com.twitter.heron.common.basics.ByteAmount;
import com.twitter.heron.api.metric.GlobalMetrics;
import com.twitter.heron.api.HeronSubmitter;
import com.twitter.heron.api.Config;


import com.nextcentury.herontest.bolts.*;
import com.nextcentury.herontest.spouts.*;
import com.twitter.heron.api.topology.BoltDeclarer;

import java.util.logging.Level;
import java.util.logging.Logger;
        


/**
 * 
 * DESIGN:
 * 
 *          create plane 'object' -> 
 *              located-activity / non-located-activity
 * 
 * normalizerBolt = normalize located-activity / non-located-activity
 * 
 * if located... 
 *      geoEnrichBolt = geo-enrichment, add hashes, etc
 *      geoAlertBolt = spatial alert processing ---> new SPOUT2 ... spatial alerts 
 * if non-located ...
 *      activityAlertBolt = activity alert processing ---> new SPOUT3 ... activity alerts
 * 
 * http://twitter.github.io/heron/api/
 * 
 */ 
public final class HeronTestTopology {

    /*
    private static long getMegabytes(long l) {
         return l * 1024 * 1024;
    }
    public static long getGigabytes(long l){
         return l * 1024 * 1024 * 1024;
    }
    */  
    
    public static final String REDIS_SERVER_ADDR = "99.99.99.31"; 
    /*
    REDIS Server…..
        $ cd redis-3.2.8
        $ nohup src/redis-server redis.conf & (port 6379 by default)
            $ sudo netstat -nlp|grep 6379 
            $ kill -9 <pid>
    REDIS Client…..
        $ cd redis-3.2.8
        $ src/redis-cli
        > SUBSCRIBE locations activity alerts
    */
    
    public static final int STREAM_MANAGERS = 5;
    public static final int BOLT_INSTANCES = 2;    
    public static final int SPOUT_INSTANCES = 1;
    
    public static void main(String[] args) throws Exception {
        
        if (args.length != 1) {
            throw new RuntimeException("Specify topology name");
        }

        
        Logger.getLogger(HeronTestTopology.class.getName()).log(Level.INFO,
                "Topology intializing auto dependency injection...\n");
        
        Logger.getLogger(HeronTestTopology.class.getName()).log(Level.INFO,
                "Topology being built....\n");

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(AirTrafficSpout.AIRTRAFFIC_DATA_SOURCE, new AirTrafficSpout(), SPOUT_INSTANCES);
        
        //normalize data into location and activity objects
        //then route that data to the appropriate streams
        builder.setBolt(NormalizerBolt.NORMALIZER_NODE, new NormalizerBolt(), BOLT_INSTANCES)
                .shuffleGrouping(AirTrafficSpout.AIRTRAFFIC_DATA_SOURCE /*use default stream ID*/);  
        builder.setBolt(PunchingBagBolt.PUNCHINGBAG_NODE, new PunchingBagBolt(), BOLT_INSTANCES)
                .shuffleGrouping(NormalizerBolt.NORMALIZER_NODE /*use default stream ID*/);  
        builder.setBolt(RouterBolt.ROUTER_NODE, new RouterBolt(), BOLT_INSTANCES)
                .shuffleGrouping(PunchingBagBolt.PUNCHINGBAG_NODE /*use default stream ID*/);        
                //.shuffleGrouping(NormalizerBolt.NORMALIZER_NODE, NormalizerBolt.NORMALIZER_STREAM);        //if punching bag removed

        //receive location objects and enrich them (if possible); 
        //send enriched location objects to the location alerting stream
        builder.setBolt(LocationEnrichBolt.LOCATION_ENRICH_NODE, new LocationEnrichBolt(), BOLT_INSTANCES)
                .shuffleGrouping(RouterBolt.ROUTER_NODE,RouterBolt.LOCATION_ROUTER_STREAM);          
        builder.setBolt(LocationAlertBolt.LOCATION_ALERT_NODE, new LocationAlertBolt(), BOLT_INSTANCES)
                .shuffleGrouping(LocationEnrichBolt.LOCATION_ENRICH_NODE /*use default stream ID*/); 
        
        //receive activity objects and enrich them (if possible); 
        //send enriched activity objects to the activity alerting stream
        builder.setBolt(ActivityEnrichBolt.ACTIVITY_ENRICH_NODE, new ActivityEnrichBolt(), BOLT_INSTANCES)
                .shuffleGrouping(RouterBolt.ROUTER_NODE,RouterBolt.ACTIVITY_ROUTER_STREAM);          
        builder.setBolt(ActivityAlertBolt.ACTIVITY_ALERT_NODE, new ActivityAlertBolt(), BOLT_INSTANCES)
                .shuffleGrouping(ActivityEnrichBolt.ACTIVITY_ENRICH_NODE /*use default stream ID*/ ); 
                      
        //alert publisher - takes in two different streams
        BoltDeclarer bdAlertPublisher = builder.setBolt(AlertPublisherBolt.ALERT_PUBLISHER_NODE, new AlertPublisherBolt(), BOLT_INSTANCES);
        bdAlertPublisher.shuffleGrouping(LocationAlertBolt.LOCATION_ALERT_NODE, LocationAlertBolt.LOCATION_ALERT_STREAM);         
        bdAlertPublisher.shuffleGrouping(ActivityAlertBolt.ACTIVITY_ALERT_NODE, ActivityAlertBolt.ACTIVITY_ALERT_STREAM);
        
        
        Config conf = new Config();
        conf.setDebug(true);
        
        try {
            conf.setSerializationClassName(KryoSerializer.class.getName());
        } catch (NoClassDefFoundError ncdfe) {
            //we'll fall back on using JAVA serialization then...
            Logger.getLogger(HeronTestTopology.class.getName()).log(Level.INFO,
                "Kryo serialization load failed - using JAVA\n");
        }          
        
        String jvmOptions = "-XX:-CMSScavengeBeforeRemark " +       //Turn off extra logging, start
                            //"-XX:-PrintGCDetails" +               //This doesn't work in some JVMs?
                            //"-XX:-PrintGCTimeStamps" +            //This doesn't work in some JVMs?
                            //"-XX:-PrintGCCause" +                 //This doesn't work in some JVMs?
                            //"-XX:-PrintPromotionFailure" +        //This doesn't work in some JVMs?
                            //"-XX:-PrintTenuringDistribution " +   //This doesn't work in some JVMs?
                            "-XX:-PrintHeapAtGC  " +                //Turn off extra logging, end
                            "-XX:-UseConcMarkSweepGC " +            //Turn off CMS
                            "-XX:+UseG1GC";                         //Java 8 - use G1 GC         
        conf.setComponentJvmOptions(AirTrafficSpout.AIRTRAFFIC_DATA_SOURCE, jvmOptions );  
        conf.setComponentJvmOptions(NormalizerBolt.NORMALIZER_NODE, jvmOptions ); 
        conf.setComponentJvmOptions(PunchingBagBolt.PUNCHINGBAG_NODE, jvmOptions );
        conf.setComponentJvmOptions(RouterBolt.ROUTER_NODE, jvmOptions );
        conf.setComponentJvmOptions(LocationEnrichBolt.LOCATION_ENRICH_NODE, jvmOptions ); 
        conf.setComponentJvmOptions(ActivityEnrichBolt.ACTIVITY_ENRICH_NODE, jvmOptions); 
        conf.setComponentJvmOptions(LocationAlertBolt.LOCATION_ALERT_NODE, jvmOptions ); 
        conf.setComponentJvmOptions(ActivityAlertBolt.ACTIVITY_ALERT_NODE, jvmOptions ); 
        conf.setComponentJvmOptions(AlertPublisherBolt.ALERT_PUBLISHER_NODE, jvmOptions ); 
                
        conf.setMaxSpoutPending(1000 * 1000 * 1000);//large number to prevent a max
        conf.setTopologyReliabilityMode(Config.TopologyReliabilityMode.ATMOST_ONCE); //Config.TopologyReliabilityMode.ATLEAST_ONCE
        conf.put(Config.TOPOLOGY_WORKER_CHILDOPTS, "-XX:+HeapDumpOnOutOfMemoryError");
        conf.setNumStmgrs(STREAM_MANAGERS); //number of stream managers        
        
        conf.setComponentRam(AirTrafficSpout.AIRTRAFFIC_DATA_SOURCE, ByteAmount.fromMegabytes(500) );  
        conf.setComponentRam(NormalizerBolt.NORMALIZER_NODE, ByteAmount.fromMegabytes(200) ); 
        conf.setComponentRam(PunchingBagBolt.PUNCHINGBAG_NODE, ByteAmount.fromMegabytes(200) );
        conf.setComponentRam(RouterBolt.ROUTER_NODE, ByteAmount.fromMegabytes(200) );
        conf.setComponentRam(LocationEnrichBolt.LOCATION_ENRICH_NODE, ByteAmount.fromMegabytes(200) ); 
        conf.setComponentRam(ActivityEnrichBolt.ACTIVITY_ENRICH_NODE, ByteAmount.fromMegabytes(200) ); 
        conf.setComponentRam(LocationAlertBolt.LOCATION_ALERT_NODE, ByteAmount.fromMegabytes(200) ); 
        conf.setComponentRam(ActivityAlertBolt.ACTIVITY_ALERT_NODE, ByteAmount.fromMegabytes(200) ); 
        conf.setComponentRam(AlertPublisherBolt.ALERT_PUBLISHER_NODE, ByteAmount.fromMegabytes(200) ); 

        conf.setContainerDiskRequested( ByteAmount.fromGigabytes(2) ); 
        conf.setContainerCpuRequested( 1 );
      
        
        HeronSubmitter.submitTopology(args[0], conf, builder.createTopology());
    }      
   
    
}
