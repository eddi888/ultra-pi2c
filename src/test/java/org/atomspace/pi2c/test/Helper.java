/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atomspace.pi2c.test;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultRoute;
import org.apache.camel.impl.InterceptSendToEndpoint;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.log4j.Logger;
import org.atomspace.pi2c.device.lm75.Lm75;
import org.atomspace.pi2c.device.lm75.Lm75Value;

public class Helper {
    
    private static Logger log = Logger.getLogger(Helper.class);
    
    public static void logContextOverview(SpringCamelContext camelContext){
        
        //Display Current loaded Routes and Endpoints for Easy Debugging
        StringBuffer buffer = new StringBuffer();
        buffer.append("\n####################################################");
        buffer.append("\n#############   Routing Overview   #################");
        buffer.append("\n####################################################");
        List<Route> routes = camelContext.getRoutes();        
        for (Route route : routes) {
            DefaultRoute defaultRoute = (DefaultRoute) route;
            
            buffer.append("\n" + defaultRoute.getId());
            buffer.append("\n     " + defaultRoute.getConsumer().getEndpoint().getEndpointUri()+ " Status:" +defaultRoute.getStatus());

        }

        buffer.append("\n####################################################");
        buffer.append("\n#############  Endpoint Overview   #################");
        buffer.append("\n####################################################");
        Collection<Endpoint> endpoints = camelContext.getEndpoints();
        for (Endpoint endpoint : endpoints) {
            buffer.append("\n" + endpoint.getEndpointKey() + " - " + endpoint.getEndpointUri());
        }
        log.info(buffer);
        
    }
    
    public static Lm75Value getLm75ValueBodyExample01(){
        Lm75Value value = new Lm75Value();
        value.setTemperature(20.5);
        value.setTemperatureAlarmStart(80.0);
        value.setTemperatureAlarmStop(75.0);
        return value;
    }
    
    public static Map<String, Object> getLm75ValueHeaderExample01(){
        return body2header(getLm75ValueBodyExample01());
    }
    
    public static Lm75Value getLm75ValueBodyExample02(){
        Lm75Value value = new Lm75Value();
        value.setTemperature(27.5);
        value.setTemperatureAlarmStart(80.0);
        value.setTemperatureAlarmStop(75.0);
        return value;
    }
    
    public static Map<String, Object> getLm75ValueHeaderExample02(){
        return body2header(getLm75ValueBodyExample02());
    }

    public static Map<String, Object> body2header(Lm75Value body){
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Lm75.TEMPERATURE, body.getTemperature());
        headers.put(Lm75.TEMPERATURE_ALARM_START, body.getTemperatureAlarmStart());
        headers.put(Lm75.TEMPERATURE_ALARM_STOP, body.getTemperatureAlarmStop());
        headers.put(Lm75.MEASUREMENT_TIME, new java.sql.Timestamp(new Date().getTime()));
        return headers;
    }
}
