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

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * RegisterServiceUnitTestCase.
 *
 * todo test secutiry
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class RegisterServiceUnitTestCase extends FrameworkTest
{
   static String OBJCLASS = BundleContext.class.getName();
   static String[] OBJCLASSES = new String[] { OBJCLASS };

   public static Test suite()
   {
      return suite(RegisterServiceUnitTestCase.class);
   }

   public RegisterServiceUnitTestCase(String name)
   {
      super(name);
   }

   public void testRegisterServiceErrors() throws Exception
   {
      String OBJCLASS = BundleContext.class.getName();
      String[] OBJCLASSES = new String[] { OBJCLASS };
      
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         try
         {
            bundleContext.registerService((String) null, new Object(), null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService((String[]) null, new Object(), null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(new String[0], new Object(), null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASS, null, null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASSES, null, null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASS, new Object(), null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASSES, new Object(), null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }

         Dictionary<String, Object> properties = new Hashtable<String, Object>();
         properties.put("case", "a");
         properties.put("CASE", "a");
         try
         {
            bundleContext.registerService(OBJCLASS, bundleContext, properties);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASSES, bundleContext, properties);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         bundle.stop();

         try
         {
            bundleContext.registerService(OBJCLASS, bundleContext, null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         
         try
         {
            bundleContext.registerService(OBJCLASSES, bundleContext, null);
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
   
   public void testRegisterServiceOBJCLASS() throws Exception
   {
      Dictionary<String, Object> properties = new Hashtable<String, Object>();
      properties.put(Constants.OBJECTCLASS, new String[] { "rubbish" });

      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();
         assertObjectClass(OBJCLASS, reference);
         registration.setProperties(properties);
         assertObjectClass(OBJCLASS, reference);
         registration.unregister();

         registration = bundleContext.registerService(OBJCLASSES, bundleContext, null);
         reference = registration.getReference();
         assertObjectClass(OBJCLASSES, reference);
         registration.setProperties(properties);
         assertObjectClass(OBJCLASSES, reference);
         registration.unregister();
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testRegisterService() throws Exception
   {
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

         registration = bundleContext.registerService(OBJCLASSES, bundleContext, null);
         reference = registration.getReference();
         actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
      }
      finally
      {
         uninstall(bundle);
      }
   }

   public void testBundleUninstall() throws Exception
   {
      Bundle bundle1 = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle1.start();
         BundleContext bundleContext = bundle1.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);

         Bundle bundle2 = addBundle("/bundles/simple/", "simple-bundle2");
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            actual = bundleContext2.getService(reference);
            assertEquals(bundleContext, actual);
         }
         finally
         {
            uninstall(bundle2);
         }

         actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
      }
      finally
      {
         uninstall(bundle1);
      }
   }

   public void testRegisteredServices() throws Exception
   {
      Bundle bundle1 = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle1.start();
         BundleContext bundleContext = bundle1.getBundleContext();
         assertNotNull(bundleContext);

         ServiceRegistration registration = bundleContext.registerService(OBJCLASS, bundleContext, null);
         ServiceReference reference = registration.getReference();
         Object actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);

         Bundle bundle2 = addBundle("/bundles/simple/", "simple-bundle2");
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            actual = bundleContext2.getService(reference);
            assertEquals(bundleContext, actual);

            ServiceReference[] registered = bundle2.getRegisteredServices();
            assertNull(registered);

            registered = bundle1.getRegisteredServices();
            assertEquals(new ServiceReference[]{reference}, registered);
         }
         finally
         {
            uninstall(bundle2);
         }

         actual = bundleContext.getService(reference);
         assertEquals(bundleContext, actual);
      }
      finally
      {
         uninstall(bundle1);
      }
   }
}
