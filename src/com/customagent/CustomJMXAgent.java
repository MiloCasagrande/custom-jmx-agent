/*
 * Copyright 2012 Milo Casagrande milo@milo.name
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.customagent;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

/**
 * Custom JMX Agent in order to resolve firewall issues with the RMI server.
 * <p>
 * The RMI server connects to the Internet using random ports.
 * 
 * @author Milo Casagrande
 */
public class CustomJMXAgent {
    // Default port for the RMI Server
    private static final String DEFAULT_REGISTRY_PORT = "7199";

    private CustomJMXAgent() {
        // Private and empty
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void premain(final String args) throws IOException {
        String hostName;
        try {
            // We take the IP address of the machine
            hostName = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException ex) {
            // If something goes wrong, fallback to the localhost
            hostName = "localhost";
        }

        // Enable random object IDs
        System.setProperty("java.rmi.server.randomIDs", "true");

        // Get the port passed as argument, or set the default one
        final int rmiRegistryPort = Integer.parseInt(System.getProperty("rmi.custom.registry.port",
                        DEFAULT_REGISTRY_PORT));
        final int rmiServerPort = Integer.parseInt(System.getProperty("rmi.custom.server.port",
                        String.valueOf(rmiRegistryPort + 1)));

        LocateRegistry.createRegistry(rmiRegistryPort);

        // Default MBeanServer
        final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

        final Map<String, Object> envMap = new HashMap<String, Object>();

        final SslRMIClientSocketFactory clientSocketFact = new SslRMIClientSocketFactory();
        final SslRMIServerSocketFactory serverSocketFact = new SslRMIServerSocketFactory();
        envMap.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFact);
        envMap.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSocketFact);

        // Create the RMI Server URL
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostName + ":" + rmiServerPort
                        + "/jndi/rmi://" + hostName + ":" + rmiRegistryPort + "/jmxrmi");
        // Create and start the connector server
        final JMXConnectorServer connectorService = JMXConnectorServerFactory.newJMXConnectorServer(url, envMap,
                        beanServer);
        connectorService.start();
    }
}
