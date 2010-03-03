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

import org.jboss.osgi.framework.bundle.OSGiBundleWrapper;
import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.service.support.SimpleServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * GetUnGetServiceUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision$
 */
public class ServiceFactoryUnitTestCase extends FrameworkTest
{
   static String OBJCLASS = BundleContext.class.getName();
   static String[] OBJCLASSES = new String[] { OBJCLASS };

   public static Test suite()
   {
      return suite(ServiceFactoryUnitTestCase.class);
   }

   public ServiceFactoryUnitTestCase(String name)
   {
      super(name);
   }

   public void testRegisterServiceFactory() throws Exception
   {
      Bundle bundleA = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundleA.start();
         BundleContext contextA = bundleA.getBundleContext();
         assertNotNull(contextA);

         SimpleServiceFactory serviceFactory = new SimpleServiceFactory(contextA);
         ServiceRegistration sregA = contextA.registerService(OBJCLASS, serviceFactory, null);
         
         ServiceReference srefA = sregA.getReference();
         Object actual = contextA.getService(srefA);
         assertEquals(contextA, actual);
         assertInstanceOf(serviceFactory.getBundle, OSGiBundleWrapper.class);
         assertEquals(bundleA.getSymbolicName(), serviceFactory.getBundle.getSymbolicName());
         assertEquals(1, serviceFactory.getCount);
         
         srefA = contextA.getServiceReference(OBJCLASS);
         actual = contextA.getService(srefA);
         assertEquals(contextA, actual);
         assertInstanceOf(serviceFactory.getBundle, OSGiBundleWrapper.class);
         assertEquals(bundleA.getSymbolicName(), serviceFactory.getBundle.getSymbolicName());
         assertEquals(1, serviceFactory.getCount);

         Bundle bundleB = addBundle("/bundles/simple/", "simple-bundle2");
         try
         {
            bundleB.start();
            BundleContext contextB = bundleB.getBundleContext();
            assertNotNull(contextB);

            ServiceReference srefB = contextB.getServiceReference(OBJCLASS);
            actual = contextB.getService(srefB);
            assertEquals(contextA, actual);
            assertInstanceOf(serviceFactory.getBundle, OSGiBundleWrapper.class);
            
            assertEquals(bundleB.getSymbolicName(), serviceFactory.getBundle.getSymbolicName());
            assertEquals(2, serviceFactory.getCount);
         }
         finally
         {
            uninstall(bundleB);
         }
      }
      finally
      {
         uninstall(bundleA);
      }
   }
   
   public void testGetServiceFactory() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, new SimpleServiceFactory(bundleContext), null);
         ServiceReference reference = registration.getReference();

         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);

         actual = bundleContext.getService(reference);
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
   
   public void testGetServiceFactoryAfterStop() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, new SimpleServiceFactory(bundleContext), null);
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
   
   public void testGetWrongInterfacesForServiceFactory() throws Exception
   {
      String[] OBJCLASSES = {String.class.getName(), BundleContext.class.getName()};
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         bundleContext.addFrameworkListener(this);
         
         ServiceRegistration registration = bundleContext.registerService(String.class.getName(), new SimpleServiceFactory(bundleContext), null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertNull("" + actual, actual);
         
         assertFrameworkEvent(FrameworkEvent.ERROR, bundle, IllegalArgumentException.class);
         
         registration = bundleContext.registerService(OBJCLASSES, new SimpleServiceFactory(bundleContext), null);
         reference = registration.getReference();
         actual = bundleContext.getService(reference);
         assertNull("" + actual, actual);
         
         assertFrameworkEvent(FrameworkEvent.ERROR, bundle, IllegalArgumentException.class);
      }
      finally
      {
         uninstall(bundle);
      }
   }
}
