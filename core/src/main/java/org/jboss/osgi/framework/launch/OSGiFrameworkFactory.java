/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.framework.launch;

// $Id$

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dependency.plugins.AbstractControllerContext;
import org.jboss.dependency.plugins.AbstractControllerContextActions;
import org.jboss.dependency.plugins.action.ControllerContextAction;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerContextActions;
import org.jboss.dependency.spi.ControllerMode;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.AbstractBootstrap;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * An impementation of an OSGi FrameworkFactory
 * 
 * @author thomas.diesler@jboss.com
 * @since 21-Aug-2009
 */
public class OSGiFrameworkFactory implements FrameworkFactory
{
   // Provide logging
   final Logger log = Logger.getLogger(OSGiFrameworkFactory.class);
   
   /** The system property used to get a bootstrap url */
   public static final String BOOTSTRAP_URL = "org.jboss.osgi.framework.bootstrap.url";

   /** The system property used to get a bootstrap path loaded from a classloader */
   public static final String BOOTSTRAP_PATH = "org.jboss.osgi.framework.bootstrap.path";

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public Framework newFramework(Map configuration)
   {
      // Bootstrap the kernel
      AbstractBootstrap bootstrap = new BasicBootstrap();
      bootstrap.run();

      Kernel kernel = bootstrap.getKernel();
      KernelController controller = preinstallKernelBeans(kernel);

      BasicXMLDeployer deployer = new BasicXMLDeployer(kernel, ControllerMode.AUTOMATIC);

      List<URL> urls = null;

      // Specified URL property
      String bootstrapURL = getProperty(BOOTSTRAP_URL);
      if (bootstrapURL != null)
      {
         try
         {
            urls = Collections.singletonList(new URL(bootstrapURL));
         }
         catch (MalformedURLException e)
         {
            throw new RuntimeException("Invalid system property " + BOOTSTRAP_URL, e);
         }
      }

      // Specified resource path
      String bootstrapPath = getProperty(BOOTSTRAP_PATH);
      if (urls == null && bootstrapPath != null)
      {
         URL url = getResourceURL(bootstrapPath);
         if (url == null)
            throw new IllegalStateException("Cannot find bootstrap: " + bootstrapPath);

         urls = Collections.singletonList(url);
      }

      // Discover the URL if not given explicitly
      if (urls == null)
      {
         // Default bootstrap paths
         List<String> bootstraps = new ArrayList<String>();
         bootstraps.add("META-INF/jboss-osgi-bootstrap.xml");
         bootstraps.add("META-INF/jboss-osgi-system-bootstrap.xml");
         bootstraps.add("META-INF/jboss-osgi-custom-bootstrap.xml");

         urls = new ArrayList<URL>();
         for (String path : bootstraps)
         {
            URL url = getResourceURL(path);
            if (url != null)
               urls.add(url);
         }
         
         if (urls.size() == 0)
            throw new IllegalStateException("Cannot find any bootstrap: " + Arrays.asList(bootstraps));
      }

      try
      {
         for (URL url : urls)
         {
            log.debug("Deploy framework bootstrap: " + url);
            deployer.deploy(url);
         }
         deployer.validate();
      }
      catch (Throwable ex)
      {
         throw new IllegalStateException("Cannot deploy bootstrap beans", ex);
      }

      ControllerContext managerContext = controller.getInstalledContext(OSGiBundleManager.BEAN_BUNDLE_MANAGER);
      if (managerContext == null)
         throw new IllegalStateException("Cannot obtain installed bean: " + OSGiBundleManager.BEAN_BUNDLE_MANAGER);

      // Get the manger and copy the configuration
      OSGiBundleManager manager = (OSGiBundleManager)managerContext.getTarget();
      if (configuration != null)
         manager.setProperties(configuration);

      return new OSGiFramework(manager);
   }

   private URL getResourceURL(String resourceName)
   {
      URL url = null;

      ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
      if (contextLoader != null)
         url = contextLoader.getResource(resourceName);

      if (url == null)
         url = getClass().getResource(resourceName);

      return url;
   }

   private KernelController preinstallKernelBeans(Kernel kernel)
   {
      KernelController controller = kernel.getController();
      ControllerContextActions actions = new AbstractControllerContextActions(new HashMap<ControllerState, ControllerContextAction>());
      try
      {
         controller.install(new AbstractControllerContext("jboss.kernel:service=KernelController", actions, null, controller));
         controller.install(new AbstractControllerContext("jboss.kernel:service=Kernel", actions, null, kernel));
      }
      catch (Throwable th)
      {
         throw new IllegalStateException("Cannot preinstall kernel bean", th);
      }
      return controller;
   }

   /**
    * Get a property
    * 
    * @param propertyName the property name
    * @return the property
    */
   private String getProperty(final String propertyName)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return System.getProperty(propertyName);

      return AccessController.doPrivileged(new PrivilegedAction<String>()
      {
         public String run()
         {
            return System.getProperty(propertyName);
         }
      });
   }
}