/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi;

// $Id: $

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.spi.framework.OSGiBootstrap;
import org.jboss.osgi.spi.framework.OSGiBootstrapProvider;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.jboss.osgi.testing.OSGiTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.launch.Framework;

/**
 * Parent for native framework tests.  
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 10-Mar-2010
 */
public abstract class NativeFrameworkTest extends OSGiTest implements ServiceListener, SynchronousBundleListener, FrameworkListener
{
   // Provide logging
   private static final Logger log = Logger.getLogger(NativeFrameworkTest.class);

   protected static Framework framework;
   protected static BundleContext context;

   private final List<FrameworkEvent> frameworkEvents = new CopyOnWriteArrayList<FrameworkEvent>();
   private final List<BundleEvent> bundleEvents = new CopyOnWriteArrayList<BundleEvent>();
   private final List<ServiceEvent> serviceEvents = new CopyOnWriteArrayList<ServiceEvent>();

   @BeforeClass
   public static void beforeClass() throws Exception
   {
      OSGiBootstrapProvider bootProvider = OSGiBootstrap.getBootstrapProvider();
      framework = bootProvider.getFramework();
      framework.start();

      // Get the system context
      context = framework.getBundleContext();
   }

   @AfterClass
   public static void afterClass() throws Exception
   {
      if (framework != null)
      {
         framework.stop();
         framework.waitForStop(2000);
         framework = null;
      }
   }

   protected void assertNoBundleEvent() throws Exception
   {
      log.debug("bundleEvents=" + bundleEvents);
      assertEquals(0, bundleEvents.size());
   }

   protected void assertBundleEvent(int type, Bundle bundle) throws Exception
   {
      waitForEvent(bundleEvents, type);

      log.debug("bundleEvents=" + bundleEvents);
      int size = bundleEvents.size();
      assertTrue("" + size, size > 0);

      if (bundle instanceof AbstractBundleState)
         bundle = ((AbstractBundleState)bundle).getBundle();

      BundleEvent foundEvent = null;
      for (int i = 0; i < bundleEvents.size(); i++)
      {
         BundleEvent aux = bundleEvents.get(i);
         if (type == aux.getType())
         {
            if (bundle.equals(aux.getSource()) && bundle.equals(aux.getBundle()))
            {
               bundleEvents.remove(aux);
               foundEvent = aux;
               break;
            }
         }
      }

      if (foundEvent == null)
         fail("Cannot find event " + ConstantsHelper.bundleEvent(type) + " from " + bundle);
   }

   @Override
   public void frameworkEvent(FrameworkEvent event)
   {
      synchronized (frameworkEvents)
      {
         log.debug("FrameworkEvent type=" + ConstantsHelper.frameworkEvent(event.getType()) + " for " + event);
         frameworkEvents.add(event);
         frameworkEvents.notifyAll();
      }
   }

   @Override
   public void bundleChanged(BundleEvent event)
   {
      synchronized (bundleEvents)
      {
         log.debug("BundleChanged type=" + ConstantsHelper.bundleEvent(event.getType()) + " for " + event);
         bundleEvents.add(event);
         bundleEvents.notifyAll();
      }
   }

   @Override
   public void serviceChanged(ServiceEvent event)
   {
      synchronized (serviceEvents)
      {
         log.debug("ServiceChanged type=" + ConstantsHelper.serviceEvent(event.getType()) + " for " + event);
         serviceEvents.add(event);
         serviceEvents.notifyAll();
      }
   }

   protected void assertNoServiceEvent() throws Exception
   {
      log.debug("serviceEvents=" + serviceEvents);
      assertEquals(0, serviceEvents.size());
   }

   protected void assertServiceEvent(int type, ServiceReference reference) throws Exception
   {
      waitForEvent(serviceEvents, type);
      log.debug("serviceEvents=" + serviceEvents);
      int size = serviceEvents.size();
      assertTrue("" + size, size > 0);
      ServiceEvent event = serviceEvents.remove(0);
      assertEquals(ConstantsHelper.serviceEvent(type), ConstantsHelper.serviceEvent(event.getType()));
      assertEquals(reference, event.getSource());
      assertEquals(reference, event.getServiceReference());
   }

   @SuppressWarnings("rawtypes")
   private void waitForEvent(List events, int type) throws InterruptedException
   {
      // Timeout for event delivery: 3 sec 
      int timeout = 30;

      boolean eventFound = false;
      while (eventFound == false && 0 < timeout)
      {
         synchronized (events)
         {
            events.wait(100);
            for (Object aux : events)
            {
               if (aux instanceof BundleEvent)
               {
                  BundleEvent event = (BundleEvent)aux;
                  if (type == event.getType())
                  {
                     eventFound = true;
                     break;
                  }
               }
               else if (aux instanceof ServiceEvent)
               {
                  ServiceEvent event = (ServiceEvent)aux;
                  if (type == event.getType())
                  {
                     eventFound = true;
                     break;
                  }
               }
               else if (aux instanceof FrameworkEvent)
               {
                  FrameworkEvent event = (FrameworkEvent)aux;
                  if (type == event.getType())
                  {
                     eventFound = true;
                     break;
                  }
               }
            }
         }
         timeout--;
      }
   }
}
