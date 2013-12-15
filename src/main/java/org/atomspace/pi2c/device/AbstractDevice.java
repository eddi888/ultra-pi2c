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
package org.atomspace.pi2c.device;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

/**
 * Wrapper Class for om.pi4j.io.i2c.I2CBus and com.pi4j.io.i2c.I2CDevice with
 * lazy loading
 * 
 */
public abstract class AbstractDevice implements Processor {
    protected I2CBus bus;
    protected I2CDevice device;

    protected int busAddress;
    protected int deviceAddress;

    public AbstractDevice(int busAddress, int deviceAddress) throws IOException {
        this.busAddress = busAddress;
        this.deviceAddress = deviceAddress;
    }

    public I2CBus getBus() throws IOException {
        if (bus == null) {
            bus = I2CFactory.getInstance(busAddress);
        }
        return bus;
    }

    public I2CDevice getDevice() throws IOException {
        if (device == null) {
            device = this.getBus().getDevice(deviceAddress);
        }
        return device;
    }

    public int read(byte[] buffer, int offset, int size) throws IOException {
        return this.getDevice().read(buffer, offset, size);
    }

    public int read(int register, byte[] buffer, int offset, int size) throws IOException {
        return this.getDevice().read(register, buffer, offset, size);
    }
    
    public int read() throws IOException {
        return this.getDevice().read();
    }

    public void write(byte oneByte) throws IOException {
        this.getDevice().write(oneByte);
    }

    public abstract void process(Exchange exchange) throws Exception;

}
