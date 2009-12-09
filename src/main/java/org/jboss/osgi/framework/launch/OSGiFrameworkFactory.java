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
   /** The system property used to get a bootstrap url */
   public static final String BOOTSTRAP_URL = "org.jboss.osgi.framework.launch.bootstrapURL";

   /** The system property used to get a bootstrap path loaded from a classloader */
   public static final String BOOTSTRAP_PATH = "org.jboss.osgi.framework.launch.bootstrapPath";
   
   @SuppressWarnings("unchecked")
   public Framework newFramework(Map configuration)
   {
      // Bootstrap the kernel
      AbstractBootstrap bootstrap = new BasicBootstrap();
      bootstrap.run();

      Kernel kernel = bootstrap.getKernel();
      KernelController controller = preinstallKernelBeans(kernel);

      BasicXMLDeployer deployer = new BasicXMLDeployer(kernel, ControllerMode.AUTOMATIC);

      URL url = null;

      // Specified urls
      String bootstrapURL = getProperty(BOOTSTRAP_URL);
      if (bootstrapURL != null)
      {
         try
         {
            url = new URL(bootstrapURL);
         }
         catch (MalformedURLException e)
         {
            throw new RuntimeException("Invalid system property " + BOOTSTRAP_URL, e);
         }
      }

      // Default bootstrap paths
      List<String> bootstraps = Arrays.asList("jboss-osgi-bootstrap.xml", "bootstrap/jboss-osgi-bootstrap.xml", "META-INF/jboss-osgi-bootstrap.xml", "META-INF/jboss-osgi-default-bootstrap.xml");

      // Specified bootstrap path
      String bootstrapPath = getProperty(BOOTSTRAP_PATH);
      if (bootstrapPath != null)
         bootstraps = Collections.singletonList(bootstrapPath);

      ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
      for (String xml : bootstraps)
      {
         if (contextLoader != null)
            url = contextLoader.getResource(xml);
         
         if (url == null)
            url = getClass().getResource(xml);

         if (url != null)
            break;
      }
      if (url == null)
         throw new IllegalStateException("Cannot find any bootstrap: " + Arrays.asList(bootstraps));

      try
      {
         deployer.deploy(url);
      }
      catch (Throwable ex)
      {
         throw new IllegalStateException("Cannot deploy bootstrap beans", ex);
      }

      ControllerContext managerContext = controller.getInstalledContext(OSGiBundleManager.BEAN_BUNDLE_MANAGER);
      if (managerContext == null)
         throw new IllegalStateException("Cannot obtain installed bean: " + OSGiBundleManager.BEAN_BUNDLE_MANAGER);

      OSGiBundleManager manager = (OSGiBundleManager)managerContext.getTarget();
      return new OSGiFramework(manager);
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