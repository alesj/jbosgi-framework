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

import static org.junit.Assert.*;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.AbstractFrameworkTest;
import org.jboss.test.osgi.service.support.BrokenServiceFactory;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
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
public class GetUnGetServiceUnitTestCase extends AbstractFrameworkTest
{
   static String OBJCLASS = BundleContext.class.getName();

   @Test
   public void testGetUnServiceErrors() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         catch (IllegalArgumentException t)
         {
            // expected
         }
         
         try
         {
            bundleContext.ungetService(null);
            fail("Should not be here!");
         }
         catch (IllegalArgumentException t)
         {
            // expected
         }
      }
      finally
      {
         bundle.uninstall();
      }
   }
   
   @Test
   public void testGetService() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         bundle.uninstall();
      }
   }
   
   @Test
   public void testGetServiceAfterStop() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         catch (IllegalStateException t)
         {
            // expected
         }
      }
      finally
      {
         bundle.uninstall();
      }
   }
   
   @Test
   public void testErrorInGetService() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.addFrameworkListener(this);
         
         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, new BrokenServiceFactory(bundleContext, true), null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertNull("" + actual, actual);
         
         assertFrameworkEvent(FrameworkEvent.ERROR, bundle, BundleException.class);
      }
      finally
      {
         bundle.uninstall();
      }
   }
   
   @Test
   public void testErrorInUnGetService() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.addFrameworkListener(this);
         
         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, new BrokenServiceFactory(bundleContext, false), null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         assertNoFrameworkEvent();

         registration.unregister();
         
         assertFrameworkEvent(FrameworkEvent.WARNING, bundle, BundleException.class);
      }
      finally
      {
         bundle.uninstall();
      }
   }

   @Test
   public void testUnGetServiceResult() throws Exception
   {
      VirtualFile assembly1 = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle1 = context.installBundle(assembly1.toURL().toExternalForm());
      try
      {
         bundle1.start();
         BundleContext bundleContext = bundle1.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
         assertFalse(bundleContext.ungetService(reference));

         bundleContext.getService(reference);
         bundleContext.getService(reference);
         assertTrue(bundleContext.ungetService(reference));
         assertFalse(bundleContext.ungetService(reference));

         VirtualFile assembly2 = assembleArchive("simple-bundle2", "/bundles/simple/simple-bundle2", new Class[0]);
         Bundle bundle2 = context.installBundle(assembly2.toURL().toExternalForm());
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
            bundle2.uninstall();
         }
      }
      finally
      {
         bundle1.uninstall();
      }
   }
}
