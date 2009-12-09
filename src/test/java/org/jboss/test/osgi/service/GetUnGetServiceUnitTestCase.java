/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
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
package org.jboss.test.osgi.service;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.service.support.BrokenServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * GetUnGetServiceUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class GetUnGetServiceUnitTestCase extends FrameworkTest
{
   static String OBJCLASS = BundleContext.class.getName();

   public static Test suite()
   {
      return suite(GetUnGetServiceUnitTestCase.class);
   }

   public GetUnGetServiceUnitTestCase(String name)
   {
      super(name);
   }

   public void testGetUnServiceErrors() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.registerService(OBJCLASS, bundleContext, null);
         
         try
         {
            bundleContext.getService(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.ungetService(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testGetService() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();

         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         
         registration.unregister();
         actual = bundleContext.getService(reference);
         assertNull("" + actual, actual);
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testGetServiceAfterStop() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();

         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         
         bundle.stop();
         try
         {
            bundleContext.getService(reference);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testErrorInGetService() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.addFrameworkListener(this);
         
         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), new BrokenServiceFactory(bundleContext, true), null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertNull("" + actual, actual);
         
         assertFrameworkEvent(FrameworkEvent.ERROR, bundle, RuntimeException.class);
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testErrorInUnGetService() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.addFrameworkListener(this);
         
         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), new BrokenServiceFactory(bundleContext, false), null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         assertNoFrameworkEvent();

         registration.unregister();
         
         assertFrameworkEvent(FrameworkEvent.WARNING, bundle, RuntimeException.class);
      }
      finally
      {
         uninstall(bundle);
      }
   }

   public void testUnGetServiceResult() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         assertFalse(bundleContext.ungetService(reference));

         bundleContext.getService(reference);
         bundleContext.getService(reference);
         assertTrue(bundleContext.ungetService(reference));
         assertFalse(bundleContext.ungetService(reference));

         Bundle bundle2 = addBundle("/bundles/simple/", "simple-bundle2");
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            bundleContext2.getService(reference);

            bundleContext.getService(reference);
            assertFalse(bundleContext.ungetService(reference));

            assertFalse(bundleContext2.ungetService(reference));
         }
         finally
         {
            uninstall(bundle2);
         }
      }
      finally
      {
         uninstall(bundle);
      }
   }
}
