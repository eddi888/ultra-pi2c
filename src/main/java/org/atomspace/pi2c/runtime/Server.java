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
package org.atomspace.pi2c.runtime;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Server implements Runnable {

    //Only a Jar is started by java -jar server.jar, have to extract and update the classpath
    static public final int RUNMODE_JAR = 1;  
    //The Jar is complete extracted and Enviroment with CLASSPATH existing
    static public final int RUNMODE_ENV = 2;    
    
    //Server startup
    static public final int STATUS_STARTING = 1;
    //Server running
    static public final int STATUS_RUNNING = 2;
    //Server receive shutdown signal and process the stopping
    static public final int STATUS_STOPING = 3;  

    //Value of RUNMODE_
    static private int runmode;
    //Value of STATUS_
    static private int status;
    
    static private boolean windows;
    static private boolean linux;
    static private boolean sunos;
    static private boolean freebsd;
    
    private URL location;
    private ClassPathXmlApplicationContext springContext;
    
    public static void main(String[] args) throws Exception {
        System.err.println("::: ----------------------------------------------------------------------- :::");
        System.err.println("::: ------------------------------  STARTING  ------------------------------:::");
        System.err.println("::: ----------------------------------------------------------------------- :::");
        System.err.println("\n::: SYSTEM-Properties:                                                    :::");
        Set<?> properties = System.getProperties().keySet();
        for (Object object : properties) {
            System.err.println("::: "+object.toString()+" = "+System.getProperty(object.toString()));
        }
        System.err.println("\n::: ENV-Properties:                                                       :::");
        properties = System.getenv().keySet();
        for (Object object : properties) {
            System.err.println("::: "+object.toString()+" = "+System.getenv(object.toString()));
        }
        
        windows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        linux = System.getProperty("os.name").toLowerCase().startsWith("linux");
        sunos = System.getProperty("os.name").toLowerCase().startsWith("sun");
        freebsd = System.getProperty("os.name").toLowerCase().startsWith("freebsd");

        if(linux || sunos){
            //USR2-Signal-Handel lookup internal Server STATUS for Linux or SunOS
            System.err.println("::: "+new Date()+"::: run unter Linux or SunOS :::");    
            addUnixSignalStatusHandle();
        }else if(freebsd){
            System.err.println("::: "+new Date()+"::: run unter FreeBSD :::");
        }else if(windows){
            //Gracefull Shutdown JFrame for Windows, because can not kill -15 <pid> on Window or in Eclipse Console
            System.err.println("::: "+new Date()+" ::: run unter windows :::");    
            addWindowsShutdownHandle();
        }else{
            System.err.println("UNKNOWN OS:" +System.getProperty("os.name"));
        }
        
        status = STATUS_STARTING;
                
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        
        //Thread can stop by Shutdown-Hook
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        
        
    }

    private static void addWindowsShutdownHandle() {
        Runnable guiCreator = new Runnable() {
            public void run() {
                // create a swing window
                JFrame macroWindow = new JFrame("MacroServer");
                macroWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                macroWindow.setSize(250, 70);
                macroWindow.setVisible(true);
                JButton button = new JButton("SHUTDOWN");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("STOPPING SERVER, PLEASE WAIT...");
                        System.exit(0);
                    }
                });
                macroWindow.getContentPane().add(button);
            }
        };
        SwingUtilities.invokeLater(guiCreator);
    }

    private static void addUnixSignalStatusHandle() {
        Signal.handle(new Signal("USR2"), new SignalHandler() {
            @Override
            public void handle(Signal sig) {
                System.err.print("::: "+new Date()+" ::: RECIVE SIGNAL "+sig+" ::: Internal Server Status: ");
                if(sig!=null){
                    if(status==STATUS_STARTING){
                        System.err.println("'STATUS_STARTING' :::");
                    }else if(status==STATUS_RUNNING){
                        System.err.println("'STATUS_RUNNING' :::");
                    }else if(status==STATUS_STOPING){
                        System.err.println("'STATUS_STOPING' :::");
                    }else{
                        System.err.println("'STATUS_UNKNOWN' :::");
                    }
                }
                System.err.flush();
            }
        });
    }

    /**
     * Run the Server Thread
     */
    @Override
    public void run() {
        location = Server.class.getProtectionDomain().getCodeSource().getLocation();
        System.err.println("::: "+new Date()+" ::: loading from '"+location+"' :::");

        //GET Lookup Enviroment
        if(location.toString().endsWith("jar")){
            runmode=RUNMODE_JAR;
            System.err.println("::: "+new Date()+" ::: Starting in JAR-MODE :::");
        }else{
            runmode=RUNMODE_ENV;
            System.err.println("::: "+new Date()+" ::: Starting in existing ENVIROMENT-MODE :::");
        }
       
        // Adding a Shutdownhook for graceful camel context stop
        ServerShutdownHook shutdownHook = new ServerShutdownHook();
        Thread shutdownHookThread = new Thread(shutdownHook);
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        
        // Extract and Loading Jars with Classloader
        try {
            if(runmode==RUNMODE_JAR) this.loadingJars();
            this.startSpringContext();
            status = STATUS_RUNNING;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starting a Spring-Context with application-context.xml in classpath
     */
   private void startSpringContext() {
       String filename = "application-context.xml";
       springContext = new ClassPathXmlApplicationContext(filename);
       springContext.start();
    }

    /**
     * Loading Jars with the ClassLoader and Unzip the included Jar-Files from libs folder
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void loadingJars() throws SecurityException, NoSuchMethodException, IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // Prepair Classloader Reflextion
        Method m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        m.setAccessible(true);

        ClassLoader cl = Server.class.getClassLoader();

        // Create Temp Dir
        File appTmp = new File(System.getProperty("java.io.tmpdir") + "/" + location.toString().substring(location.toString().lastIndexOf("/")));
        System.err.println("::: use temporary dir path '"+appTmp.getAbsolutePath()+"'");
        if (!appTmp.exists()) {
            appTmp.mkdir();
        }

        
        ZipInputStream zip = new ZipInputStream(location.openStream());
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            if (entry.getName().startsWith("libs/") && entry.getName().endsWith(".jar") || (entry.getName().endsWith("MANIFEST.MF"))) {
                System.err.println("::: "+new Date()+" ::: Extracting " + entry.getName()+" :::");
                InputStream is = cl.getResourceAsStream(entry.getName());
                File tmpJar = new File(appTmp + "/" + entry.getName().substring(entry.getName().lastIndexOf("/")));
                unzip(is, tmpJar);
                m.invoke(cl, new Object[] { tmpJar.toURI().toURL() });
            } else {
                System.err.println("::: "+new Date()+" ::: ignore " + entry.getName()+" :::");

            }
            entry = zip.getNextEntry();
        }
    }

    /**
     * Unzip a InputStream to a File-Destination 
     * @param src
     * @param destination
     * @throws IOException
     */
    private void unzip(InputStream src, File destination) throws IOException {
        FileOutputStream fos = new FileOutputStream(destination);

        int len = 0;
        byte[] buffer = new byte[16384];

        while ((len = src.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.flush();
        fos.close();
    }
    

    /**
     * Shutdown-Hook for Stop the Spring-Context graceful
     */
    private class ServerShutdownHook implements Runnable {
        
        @Override
        public void run() {
            status = STATUS_STOPING;
            
            System.err.println("::: "+new Date()+" ::: Shutdown from: "+location+" :::");
            if(springContext==null){
                System.err.println("::: "+new Date()+" ::: SpringContext not existing only stop :::");
            }else{
                System.err.println("::: "+new Date()+" ::: SpringContext isActive:" + springContext.isActive() + " / SpringContext isRunning:" + springContext.isRunning()+" :::");
                springContext.stop();
                System.err.println("::: "+new Date()+" ::: SpringContext isActive:" + springContext.isActive() + " / SpringContext isRunning:" + springContext.isRunning()+" :::");
                springContext.destroy();
                System.err.println("::: "+new Date()+" ::: SpringContext isActive:" + springContext.isActive() + " / SpringContext isRunning:" + springContext.isRunning()+" :::");
            }
            System.err.println("::: ----------------------------------------------------------------------- :::");
            System.err.println("::: ------------------------------  STOPPED  ------------------------------ :::");
            System.err.println("::: ----------------------------------------------------------------------- :::");
        }
    }

}

