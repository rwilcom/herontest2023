package com.nextcentury.utils;

import java.io.Serializable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * 
 * 
 * 
 * 
 * 
 */
@Configuration
@ComponentScan
@PropertySource("classpath:pubsub.properties")
public class PubSubConfig implements Serializable {
  
    @Value("${connect.url}")
    private String serverUri;
    
    @Value("${channelID.locations}")
    private String locationsChannelID;
 
    @Value("${channelID.activity}")
    private String activityChannelID;

    @Value("${channelID.alerts}")
    private String alertsChannelID;
    
    @Value("${channelID.punchingBag}")
    private String punchingBagChannelID;

    public String getServerUri() {
        return serverUri;
    }

    public String getLocationsChannelID() {
        return locationsChannelID;
    }

    public String getActivityChannelID() {
        return activityChannelID;
    }

    public String getAlertsChannelID() {
        return alertsChannelID;
    }

    public String getPunchingBagChannelID() {
        return punchingBagChannelID;
    }
    
    //To resolve ${} in @Value
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
            return new PropertySourcesPlaceholderConfigurer();
    }
}    
    
