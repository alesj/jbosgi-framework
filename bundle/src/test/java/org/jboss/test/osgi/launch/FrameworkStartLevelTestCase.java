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
package org.jboss.test.osgi.launch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.jboss.osgi.testing.OSGiFrameworkTest;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class FrameworkStartLevelTestCase extends OSGiFrameworkTest
{
   @Test
   public void testFrameworkShutdown() throws Exception
   {
      // We expect 2 started events...
      final CountDownLatch latch = new CountDownLatch(2);
      BundleListener bl = new BundleListener()
      {
         @Override
         public void bundleChanged(BundleEvent event)
         {
            if (event.getType() == BundleEvent.STARTED)
               latch.countDown();
         }
      };
      BundleContext ctx = getFramework().getBundleContext();
      ctx.addBundleListener(bl);

      ServiceReference sref = ctx.getServiceReference(StartLevel.class.getName());
      StartLevel sls = (StartLevel)ctx.getService(sref);

      Archive<?> assA = assembleArchive("lifecycle-order1", "/bundles/lifecycle/order01",
            org.jboss.test.osgi.bundle.support.lifecycle1.Activator.class);
      Bundle ba = installBundle(assA);
      sls.setBundleStartLevel(ba, 3);

      Archive<?> assB = assembleArchive("lifecycle-order2", "/bundles/lifecycle/order02",
            org.jboss.test.osgi.bundle.support.lifecycle2.Activator.class);
      Bundle bb = installBundle(assB);
      sls.setBundleStartLevel(bb, 2);

      ba.start();
      bb.start();
      sls.setStartLevel(5);

      latch.await(10, SECONDS);
      assertEquals("start2start1", System.getProperty("LifecycleOrdering"));

      getFramework().stop();
      getFramework().waitForStop(10000);
      assertEquals("start2start1stop1stop2", System.getProperty("LifecycleOrdering"));
   }
}
