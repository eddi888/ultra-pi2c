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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Lm75Value implements Serializable {
    
    private static final long serialVersionUID = 1L;

    //temperature
    private Double temperature;
    
    //configuration
    private Config config;
    
    //temperature at which the alarm condition goes away (THYST).
    private Double temperatureAlarmStop;
    
    //temperature alarm threshold (TOS) for the  Overtemperature Shutdown Open Collector Output.
    private Double temperatureAlarmStart;
    
    
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Config getConfig() {
        if(config==null){
            config = new Config();
        }
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Double getTemperatureAlarmStop() {
        return temperatureAlarmStop;
    }

    public void setTemperatureAlarmStop(Double temperatureAlarmStop) {
        this.temperatureAlarmStop = temperatureAlarmStop;
    }

    public Double getTemperatureAlarmStart() {
        return temperatureAlarmStart;
    }

    public void setTemperatureAlarmStart(Double temperatureAlarmStart) {
        this.temperatureAlarmStart = temperatureAlarmStart;
    }

    public enum Mode {
        COMPARATOR, INTERRUPT
    }
    
    public enum Polarity {
        ACTIVE_LOW, ACTIVE_HIGH
    }
    
    public class Config  implements Serializable {
        
        private static final long serialVersionUID = 1L;

        //Shutdown: When set to 1 the LM75 goes to low power shutdown mode.
        private boolean shutdown;
        
        //Comparator/Interrupt mode: 0 is Comparator mode, 1 is Interrupt mode
        private Mode mode;
        
        //O.S. Polarity: 0 is active low, 1 is active high. O.S. is an open-drain output under all conditions.
        private Polarity osPolarity;

        //Fault Queue: Number of faults necessary to detect before setting O.S. output to avoid false tripping due to noise:
        private int faultQueueNumbers;

        public boolean isShutdown() {
            return shutdown;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public Polarity getOsPolarity() {
            return osPolarity;
        }

        public void setOsPolarity(Polarity osPolarity) {
            this.osPolarity = osPolarity;
        }

        public int getFaultQueueNumbers() {
            return faultQueueNumbers;
        }

        public void setFaultQueueNumbers(int faultQueueNumbers) {
            this.faultQueueNumbers = faultQueueNumbers;
        }
        
    }    
}


