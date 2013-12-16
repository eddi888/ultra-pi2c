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
package org.atomspace.pi2c.device.lm75;

import java.io.IOException;




import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.log4j.Logger;
import org.atomspace.pi2c.device.AbstractDevice;
import org.atomspace.pi2c.device.lm75.Lm75Value.Config;
import org.atomspace.pi2c.device.lm75.Lm75Value.Mode;
import org.atomspace.pi2c.device.lm75.Lm75Value.Polarity;

/**
 * LM75 Temperatur Sensor
 * 
 */
public class Lm75 extends AbstractDevice {

    final public static String TEMPERATURE = "temperature";
    final public static String TEMPERATURE_ALARM_START = "temperatureAlarmStart";
    final public static String TEMPERATURE_ALARM_STOP = "temperatureAlarmStop";
    final public static String MEASUREMENT_TIME = "measurementTime";
    
    private static Logger log = Logger.getLogger(Lm75.class);
    
    boolean switchEnable = false;

    public Lm75(int busAddress, int deviceAddress) throws IOException {
        super(busAddress, deviceAddress);
    }

    void setSwitch(boolean enable) {
        switchEnable = enable;
    }

    boolean isSwitchEnable() {
        return switchEnable;
    }

    /**
     * read current temperatur in celsius from register 00 
     * @return
     * @throws IOException
     */
    double readTemperatur() throws IOException {
        byte[] buffer = new byte[2];
        int readedBytes = this.read(0, buffer, 0, 2);
        
        log.trace("readTemperatur readedBytes: " + readedBytes);

        return convertBytesToTemperatur(buffer);
    }

    /**
     * read current configuration from register 01 
     * @return
     * @throws IOException
     */
    Config readConfiguration() throws IOException {
        byte[] buffer = new byte[1];
        int readedBytes = this.read(1,buffer, 0, 1);
        log.trace("readConfiguration readedBytes: " + readedBytes);
        if(readedBytes>0) log.trace("BUFFER0:"+ buffer[0]);;
        
        return convertBytesToConfig(buffer);
    }
    
    /**
     * read current Tos-Status from register 03 
     * @return
     * @throws IOException
     */
    double readTos() throws IOException {

        byte[] buffer = new byte[2];
        int readedBytes = this.read(3,buffer, 0, 2);
        log.trace("readTos readedBytes: " + readedBytes);
        if(readedBytes>0) log.trace("BUFFER0:"+ buffer[0]);;
        if(readedBytes>1) log.trace("BUFFER1:"+  buffer[1]);;
        
        return convertBytesToTemperatur(buffer);
    }
    
    /**
     * read current Thyst-Status from register 02 
     * @return
     * @throws IOException
     */
    double readThyst() throws IOException {

        byte[] buffer = new byte[2];
        int readedBytes = this.read(2,buffer, 0, 2);
        log.trace("readThyst readedBytes: " + readedBytes);
        if(readedBytes>0) log.trace("BUFFER0:"+ buffer[0]);;
        if(readedBytes>1) log.trace("BUFFER1:"+  buffer[1]);;
        
        return convertBytesToTemperatur(buffer);
    }

    private double convertBytesToTemperatur(byte[] buffer){
        int x=0;
        double y=0;
        x = buffer[0];
        if ((buffer[0] & 128) == 128) {
            x = x - 256;
        }
        if ((buffer[1] & 128) == 128) {
            y = y + 0.5;
        }
        return x + y;
    }
    
    private Config convertBytesToConfig(byte[] buffer){
        /*
        1   2   4   8  16        32        64       128
        D7  D6  D5  D4 D3        D2        D1       D0
        0   0   0   Fault Queue  O.S.      Cmp/Int  Shutdown
                                 Polarity
        */
        Config config = new Lm75Value().getConfig();
        
        if ((buffer[0] & 128) == 128){
            config.setShutdown(true);
        }else{
            config.setShutdown(false);   
        }
        
        if ((buffer[0] & 64) == 64){
            config.setMode(Mode.INTERRUPT);
        }else{
            config.setMode(Mode.COMPARATOR);   
        }
        
        if ((buffer[0] & 32) == 32){
            config.setOsPolarity(Polarity.ACTIVE_HIGH);
        }else{
            config.setOsPolarity(Polarity.ACTIVE_LOW);   
        }
        
        //TODO Fault Queue
        
        return config;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        Message out = exchange.getOut();

        out.setHeader(TEMPERATURE, this.readTemperatur());
        out.setHeader(TEMPERATURE_ALARM_START, this.readTos());
        out.setHeader(TEMPERATURE_ALARM_STOP, this.readThyst());
        out.setHeader(MEASUREMENT_TIME, new java.sql.Timestamp(new Date().getTime()));
        
        Lm75Value value = new Lm75Value();
        value.setTemperature(this.readTemperatur());
        value.setTemperatureAlarmStart(this.readTos());
        value.setTemperatureAlarmStop(this.readThyst());
        value.setConfig(this.readConfiguration());
        out.setBody(value);

    }

}
