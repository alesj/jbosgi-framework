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
package org.jboss.osgi.framework.plugins.internal;

//$Id$

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.AutoInstallPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A plugin that installs/starts bundles on framework startup.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Aug-2009
 */
public class AutoInstallPluginImpl extends AbstractPlugin implements AutoInstallPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(AutoInstallPluginImpl.class);
   
   private List<URL> autoInstall = Collections.emptyList();
   private List<URL> autoStart = Collections.emptyList();
   
   private Map<URL, Bundle> autoBundles = new ConcurrentHashMap<URL, Bundle>();
   
   public AutoInstallPluginImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   public void setAutoInstall(List<URL> autoInstall)
   {
      this.autoInstall = autoInstall;
   }

   public void setAutoStart(List<URL> autoStart)
   {
      this.autoStart = autoStart;
   }

   public void installBundles() throws BundleException
   {
      // Add the autoStart bundles to autoInstall
      for (URL bundleURL : autoStart)
      {
         autoInstall.add(bundleURL);
      }

      // Install autoInstall bundles
      for (URL bundleURL : autoInstall)
      {
         
         Bundle bundle = bundleManager.installBundle(bundleURL);
         autoBundles.put(bundleURL, bundle);
      }
   }

   public void startBundles() throws BundleException
   {
      // Start autoStart bundles
      for (URL bundleURL : autoStart)
      {
         Bundle bundle = autoBundles.get(bundleURL);
         if (bundle != null)
         {
            bundle.start();
         }
      }
   }
}