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

import java.util.Hashtable;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.service.support.SimpleServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * ServiceRegistrationUnitTestCase.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class ServiceRegistrationUnitTestCase extends FrameworkTest
{
   public ServiceRegistrationUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(ServiceRegistrationUnitTestCase.class);
   }

   public void testGetReference() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, null);
         assertNotNull(registration);
         
         ServiceReference reference = registration.getReference();
         assertNotNull(reference);

         ServiceReference reference2 = bundleContext.getServiceReference(BundleContext.class.getName());
         assertEquals(reference, reference2);
         
         Object object = bundleContext.getService(reference);
         assertEquals(bundleContext, object);

         reference2 = registration.getReference();
         assertEquals(reference, reference2);
         
         registration.unregister();
         try
         {
            registration.getReference();
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }

         ServiceRegistration registration2 = bundleContext.registerService(BundleContext.class.getName(), bundleContext, null);
         assertNotNull(registration);
         assertNotSame(registration, registration2);
         
         reference2 = registration2.getReference();
         assertNotNull(reference2);
         assertNotSame(reference, reference2);
         
         bundle.stop();
         try
         {
            registration2.getReference();
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
   
   public void testSetProperties() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         String propertyA = "org.jboss.osgi.test.PropertyA";
         String propertyALower = "org.jboss.osgi.test.propertya";
         
         Hashtable<String, Object> properties = new Hashtable<String, Object>();
         properties.put(propertyA, "testA");
         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, properties);
         assertNotNull(registration);
         ServiceReference reference = registration.getReference();
         assertNotNull(reference);
         assertEquals("testA", reference.getProperty(propertyA));
         assertEquals("testA", reference.getProperty(propertyALower));
         
         Object serviceID = reference.getProperty(Constants.SERVICE_ID);
         Object objectClass = reference.getProperty(Constants.OBJECTCLASS);

         assertAllReferences(bundleContext, null, "(" + propertyA + "=testA)", reference);
         assertAllReferences(bundleContext, null, "(" + propertyALower + "=testA)", reference);
         assertAllReferences(bundleContext, null, "(" + Constants.SERVICE_ID + "=" + serviceID + ")", reference);

         bundleContext.addServiceListener(this);
         
         properties = new Hashtable<String, Object>();
         properties.put(propertyA, "testAChanged");
         registration.setProperties(properties);
         assertServiceEvent(ServiceEvent.MODIFIED, reference);
         assertEquals("testAChanged", reference.getProperty(propertyA));
         assertNoAllReferences(bundleContext, null, "(" + propertyA + "=testA)");
         assertNoAllReferences(bundleContext, null, "(" + propertyALower + "=testA)");
         assertAllReferences(bundleContext, null, "(" + propertyA + "=testAChanged)", reference);
         assertAllReferences(bundleContext, null, "(" + propertyALower + "=testAChanged)", reference);
         
         registration.setProperties(null);
         assertServiceEvent(ServiceEvent.MODIFIED, reference);
         assertNull(reference.getProperty(propertyA));
         assertNoAllReferences(bundleContext, null, "(" + propertyA + "=testA)");
         assertNoAllReferences(bundleContext, null, "(" + propertyALower + "=testA)");
         assertNoAllReferences(bundleContext, null, "(" + propertyA + "=testAChanged)");
         assertNoAllReferences(bundleContext, null, "(" + propertyALower + "=testAChanged)");
         
         properties = new Hashtable<String, Object>();
         properties.put(propertyA, "testA2");
         properties.put(Constants.SERVICE_ID, "rubbish1");
         properties.put(Constants.OBJECTCLASS, "rubbish2");
         registration.setProperties(properties);
         assertServiceEvent(ServiceEvent.MODIFIED, reference);
         assertEquals("testA2", reference.getProperty(propertyA));
         assertEquals("testA2", reference.getProperty(propertyALower));
         assertEquals(serviceID, reference.getProperty(Constants.SERVICE_ID));
         assertEquals(serviceID, reference.getProperty(Constants.SERVICE_ID.toLowerCase()));
         assertEquals(objectClass, reference.getProperty(Constants.OBJECTCLASS));
         assertEquals(objectClass, reference.getProperty(Constants.OBJECTCLASS.toLowerCase()));
         
         try
         {
            assertNoAllReferences(bundleContext, null, "(" + Constants.SERVICE_ID + "=rubbish1)");
            fail("NumberFormatException expected");
         }
         catch (NumberFormatException ex)
         {
            // expected
         }
         
         assertAllReferences(bundleContext, null, "(" + Constants.SERVICE_ID + "=" + serviceID + ")", reference);

         properties = new Hashtable<String, Object>();
         properties.put("a", "1");
         properties.put("A", "2");
         try
         {
            registration.setProperties(properties);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         assertNoServiceEvent();
         
         registration.unregister();
         assertServiceEvent(ServiceEvent.UNREGISTERING, reference);
         
         try
         {
            registration.setProperties(new Hashtable<String, Object>());
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         assertNoServiceEvent();
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testSetPropertiesAfterStop() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, null);
         assertNotNull(registration);

         bundle.stop();
         
         try
         {
            registration.setProperties(new Hashtable<String, Object>());
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         assertNoServiceEvent();
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testUnregister() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         SimpleServiceFactory factory = new SimpleServiceFactory(bundleContext);
         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), factory, null);
         assertNotNull(registration);
         
         ServiceReference reference = registration.getReference();
         assertNotNull(reference);

         ServiceReference reference2 = bundleContext.getServiceReference(BundleContext.class.getName());
         assertEquals(reference, reference2);

         ServiceReference[] inUse = bundle.getServicesInUse();
         assertNull(inUse);
         
         bundleContext.getService(reference);
         inUse = bundle.getServicesInUse();
         assertEquals(new ServiceReference[] { reference }, inUse);

         Bundle bundle2 = addBundle("/bundles/simple/", "simple-bundle2");
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);
            bundleContext2.getService(reference);
            inUse = bundle2.getServicesInUse();
            assertEquals(new ServiceReference[] { reference }, inUse);

            assertNull(factory.ungetBundle);
            assertNull(factory.ungetRegistration);
            assertNull(factory.ungetService);
            
            bundleContext.addServiceListener(this);
            registration.unregister();

            reference2 = bundleContext.getServiceReference(BundleContext.class.getName());
            assertNull("" + reference2, reference2);
            
            Object actual = bundleContext.getService(reference);
            assertNull("" + actual, actual);
            
            assertServiceEvent(ServiceEvent.UNREGISTERING, reference);

            inUse = bundle.getServicesInUse();
            assertNull(inUse);
            inUse = bundle2.getServicesInUse();
            assertNull(inUse);

            assertEquals(registration, factory.ungetRegistration);
            assertEquals(bundleContext, factory.ungetService);
         }
         finally
         {
            uninstall(bundle2);
         }
         
         try
         {
            registration.unregister();
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
   
   public void testUnregisterAfterStop() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, null);
         assertNotNull(registration);

         bundle.stop();
         
         try
         {
            registration.unregister();
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
}