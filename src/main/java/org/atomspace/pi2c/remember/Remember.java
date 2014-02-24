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
package org.atomspace.pi2c.remember;

import java.sql.Timestamp;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.atomspace.pi2c.device.lm75.Lm75;

public class Remember implements Processor {
    
    private double temperature=0.0;
    private Timestamp measurementTime=null;

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public Timestamp getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(Timestamp measurementTime) {
        this.measurementTime = measurementTime;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        this.temperature = exchange.getIn().getHeader(Lm75.TEMPERATURE, Double.class);
        this.measurementTime = exchange.getIn().getHeader(Lm75.MEASUREMENT_TIME, java.sql.Timestamp.class);
    }
    
}
