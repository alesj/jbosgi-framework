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
package org.jboss.test.osgi.core.jbosg331;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.test.osgi.bundle.support.y.Activator;
import org.jboss.test.osgi.service.AbstractServiceMixTest;
import org.junit.Assume;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * [JBOSGI-331] Activator cannot change bundle start level
 * 
 * https://jira.jboss.org/browse/JBOSGI-331
 * 
 * This test isolates the failure observed by the Start Level Service OSGi TCK test
 * StartLevelControl.testActivatorChangeBundleStartLevel()
 * 
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class OSGi331TestCase extends AbstractServiceMixTest
{
   @Test
   public void testStopBundleFromWithinStartProcess() throws Exception
   {
      System.out.println("FIXME [JBOSGI-331] Activator cannot change bundle start level");
      Assume.assumeTrue(false);

      final Set<String> s = Collections.synchronizedSet(new HashSet<String>());
      BundleListener bl = new BundleListener()
      {
         public void bundleChanged(BundleEvent event)
         {
            if (event.getBundle().getSymbolicName().equals("lifecycle-bundle-stop-in-activator"))
            {
               synchronized (s)
               {
                  switch (event.getType())
                  {
                     case BundleEvent.STARTED:
                        s.add("started");
                        break;
                     case BundleEvent.STOPPED:
                        s.add("stopped");
                        break;
                  }
                  s.notifyAll();
               }
            }
         }
      };
      getSystemContext().addBundleListener(bl);

      Archive<?> assembly = assembleArchive("some_bundle", "/bundles/lifecycle/bundle-stop-in-activator", Activator.class);
      Bundle b = installBundle(assembly);
      b.start();

      waitForValue(s, "started", 10000);
      waitForValue(s, "stopped", 10000);
   }

   private void waitForValue(Set<String> s, String value, long timeout) throws Exception
   {
      synchronized (s)
      {
         if (s.contains(value))
            return;

         s.wait(timeout);
         if (s.contains(value))
            return;

         fail("Set " + s + " does not contain value '" + value + "' after timeout of " + timeout + " ms.");
      }
   }
}
