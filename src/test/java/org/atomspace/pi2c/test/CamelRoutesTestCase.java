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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Route;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultRoute;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.log4j.Logger;
import org.atomspace.pi2c.device.lm75.Lm75;
import org.atomspace.pi2c.device.lm75.Lm75Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class CamelRoutesTestCase extends CamelSpringTestSupport {
    
    private static Logger log = Logger.getLogger(CamelRoutesTestCase.class);
    
    private SpringCamelContext camelContext;
            
    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("application-test-context.xml");
    }
    
    
    public Answer<Void> lm75Answer = new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            Object[] objects = invocation.getArguments();
            Exchange exchange = (Exchange) objects[0];
            Message out = exchange.getOut();

            out.setHeader(Lm75.TEMPERATURE, 20.5);
            out.setHeader(Lm75.TEMPERATURE_ALARM_START, 80.0);
            out.setHeader(Lm75.TEMPERATURE_ALARM_STOP, 75.0);
            out.setHeader(Lm75.MEASUREMENT_TIME, new java.sql.Timestamp(new Date().getTime()));
            
            Lm75Value value = new Lm75Value();
            value.setTemperature(20.5);
            value.setTemperatureAlarmStart(80.0);
            value.setTemperatureAlarmStop(75.0);
            out.setBody(value);

            return null;
        }
    };

    @Before
    public void before() throws Exception {     
        Lm75 lm75 = (Lm75) applicationContext.getBean("lm75room");
        doAnswer(lm75Answer).when(lm75).process(any(Exchange.class));

        
        camelContext = (SpringCamelContext) applicationContext.getBean("camel-context-pi2c");

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
            DefaultEndpoint defaultEndpoint = (DefaultEndpoint) endpoint;
            buffer.append("\n" + defaultEndpoint.getEndpointUri());
        }
        
        log.info(buffer);
    }

    @Test
    public void testRequestResponseJson() throws Exception {
        String jsonString = template.requestBodyAndHeader("jetty:http://0.0.0.0:8999/room", "", "mid", "123123123", String.class);
        ObjectMapper mapper = new ObjectMapper();
        Lm75Value value = mapper.readValue(jsonString, Lm75Value.class);
        
        assertNotNull(value);
        assertEquals(new Double(20.5),value.getTemperature());
        assertEquals(new Double(80.0),value.getTemperatureAlarmStart());
        assertEquals(new Double(75.0),value.getTemperatureAlarmStop());
        assertNotNull(value.getConfig());
    }
    
    @Test
    public void testRequestResponseBrowser() throws Exception {
        String browserString = template.requestBodyAndHeader("jetty:http://0.0.0.0:8999/view-room-temperatur", "", "mid", "234234234", String.class);
        assertNotNull(browserString);
        assertTrue(browserString.indexOf("was the temperature 20,5")>=0);
    }
    
    @Test
    public void testRoutePushTemperature() throws Exception {
        camelContext.stopRoute("route-twitter-message"); //Not Consume Message from Queue "twitter-temperature"
        
        if(((DefaultRoute)camelContext.getRoute("route-twitter-message")).getStatus().isStopped()){
            
            template.sendBodyAndHeader("jetty:http://0.0.0.0:8999/twitter-room-temperature", "", "mid", "345345345");
            
            Exchange ex = consumer.receive("activemq:twitter-temperature",5000);
            Message in = ex.getIn();
            assertEquals(new Double(20.5), in.getHeader(Lm75.TEMPERATURE));
            assertEquals(new Double(80.0), in.getHeader(Lm75.TEMPERATURE_ALARM_START));
            assertEquals(new Double(75.0), in.getHeader(Lm75.TEMPERATURE_ALARM_STOP));
           
        }else{
            throw new Exception("Route 'route-twitter-message' is not stopped");
        }
    }

}
