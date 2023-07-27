package com.nextcentury.utils;

import java.io.Serializable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import org.springframework.context.annotation.Scope;


/**
 * 
 * 
 * 
 * 
 * 
 */
@Service
@Scope("prototype")
@Lazy
public class PubSubService implements Serializable {

    private static final long serialVersionUID = -1L;
    private JedisPool jedisPool;
    
    @Resource
    PubSubConfig config;
    
    public PubSubService(){
        
    }
    
    /**
     * 
     */
    public void startPublishing() {
        startPublishing( null ); 
    } 
    /**
     * 
     * @param serverUri
     */
    public void startPublishing( String serverUri ) {
       if( serverUri==null ){
           serverUri = config.getServerUri();
       } 
       
       jedisPool = new JedisPool(new JedisPoolConfig(), serverUri ); //default port:6379 
    }    
    /**
     * 
     */
    public void endPublishing( ) {
    
        if( jedisPool!=null){
            jedisPool.destroy();
        }
    }
    
    /**
     * 
     * @param channelID
     * @param message
     * @throws Exception 
     */
    public void publish( String channelID, String message ) throws Exception{
        
        try (Jedis jedis = jedisPool.getResource()) {
            
            jedis.publish(channelID, message); 
            
        }catch(Exception any){
            throw new Exception(any);
        }
    }    
    
    
    
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    
    /**
     * 
     * @param channelID
     * @param subscriber 
     */
    public void startSubscription( String channelID, JedisPubSub subscriber ) {
        startSubscription( null/*configured*/, channelID, subscriber); 
    }
    
    /**
     * 
     * @param serverUri
     * @param subscriber
     * @param channelID 
     */
    public void startSubscription( String serverUri, String channelID, JedisPubSub subscriber ) {

       if( serverUri==null ){
           serverUri = config.getServerUri();
       } 
       
       jedisPool = new JedisPool(new JedisPoolConfig(), serverUri ); //default port:6379 
       
       //need to subscribe on a new thread - 'subscibe' is a blokcking op

       final String finalChannelID = channelID;       
       new Thread(new Runnable() {
            @Override
            public void run() {
                try (Jedis jedis = jedisPool.getResource()) {         
                    jedis.subscribe(subscriber, finalChannelID);
                } catch (Exception any) {
                    Logger.getLogger(PubSubService.class.getName()).log(Level.WARNING,
                        "problem subsrcibing to pub/sub :"+any.toString()+"\n");
                }
            }
        }).start();       
    }
    
    /**
     * 
     * @param subscriber 
     */
    public void endSubscription( JedisPubSub subscriber  ) {
    
        if( subscriber!=null){
            subscriber.unsubscribe();
        }
        if( jedisPool!=null){
            jedisPool.destroy();
        }
    }
}    
    
