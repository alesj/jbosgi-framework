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
package org.jboss.test.osgi.bundle;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

import junit.framework.Test;

import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.test.osgi.FrameworkTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * BundleContextUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class BundleContextUnitTestCase extends FrameworkTest
{
   public static Test suite()
   {
      return suite(BundleContextUnitTestCase.class);
   }

   public BundleContextUnitTestCase(String name)
   {
      super(name);
   }

   public void testGetBundle() throws Exception
   {
      Bundle bundle1 = addBundle("/bundles/simple/", "simple-bundle1");
      BundleContext context1 = null;
      try
      {
         bundle1.start();
         context1 = bundle1.getBundleContext();
         assertEquals(bundle1, context1.getBundle());
         assertEquals(bundle1, context1.getBundle(bundle1.getBundleId()));
         
         Bundle[] bundles = context1.getBundles();
         Set<Bundle> actual = new HashSet<Bundle>(Arrays.asList(bundles));
         Set<Bundle> expected = new HashSet<Bundle>(Arrays.asList(bundle1));
         addBaseBundles(expected);
         assertEquals(expected, actual);
         
         Bundle bundle2 = addBundle("/bundles/simple/", "simple-bundle2");
         BundleContext context2 = null;
         try
         {
            bundle2.start();
            context2 = bundle2.getBundleContext();
            assertEquals(bundle2, context2.getBundle());
            
            bundles = context1.getBundles();
            actual = new HashSet<Bundle>(Arrays.asList(bundles));
            expected = new HashSet<Bundle>(Arrays.asList(bundle1, bundle2));
            addBaseBundles(expected);
            assertEquals(expected, actual);
            
            assertEquals(bundle1, context2.getBundle(bundle1.getBundleId()));
            assertEquals(bundle2, context1.getBundle(bundle2.getBundleId()));
         }
         finally
         {
            uninstall(bundle2);
         }

         assertEquals(bundle1, context1.getBundle(bundle1.getBundleId()));
         assertNull(context1.getBundle(bundle2.getBundleId()));
         
         bundles = context1.getBundles();
         actual = new HashSet<Bundle>(Arrays.asList(bundles));
         expected = new HashSet<Bundle>(Arrays.asList(bundle1));
         addBaseBundles(expected);
         assertEquals(expected, actual);
         
         try
         {
            context2.getBundle();
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         
         try
         {
            context2.getBundle(bundle1.getBundleId());
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         
         try
         {
            context2.getBundles();
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
      }
      finally
      {
         uninstall(bundle1);
      }
      
      try
      {
         context1.getBundle();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
      
      try
      {
         context1.getBundle(bundle1.getBundleId());
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
      
      try
      {
         context1.getBundles();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
         
   public void testProperties() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         assertEquals("r4v42", bundleContext.getProperty(Constants.FRAMEWORK_VERSION)); 
         assertEquals("jboss.org", bundleContext.getProperty(Constants.FRAMEWORK_VENDOR));
         assertEquals(Locale.getDefault().getISO3Language(), bundleContext.getProperty(Constants.FRAMEWORK_LANGUAGE));
         assertSystemProperty(bundleContext, "os.name", Constants.FRAMEWORK_OS_NAME);
         assertSystemProperty(bundleContext, "os.version", Constants.FRAMEWORK_OS_VERSION);
         assertSystemProperty(bundleContext, "os.arch", Constants.FRAMEWORK_PROCESSOR);
         
         assertNull(bundleContext.getProperty(getClass().getName()));
         System.setProperty(getClass().getName(), "test");
         assertEquals("test", bundleContext.getProperty(getClass().getName()));

         bundle.stop();
         try
         {
            bundleContext.getProperty(getClass().getName());
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
   
   public void testInstallBundle() throws Exception
   {
      OSGiTestHelper helper = new OSGiTestHelper();
      BundleContext context = getBundleManager().getSystemContext();
      
      // Test URL location
      URL url = helper.getTestArchiveURL("bundles/jboss-osgi-common.jar");
      Bundle bundle = context.installBundle(url.toExternalForm());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundle.getState());
         assertEquals(url.toExternalForm(), bundle.getLocation());
      }
      finally
      {
         bundle.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundle.getState());
      }
      
      // Test file location
      String location = helper.getTestArchivePath("bundles/jboss-osgi-common.jar");
      bundle = context.installBundle(location);
      try
      {
         assertBundleState(Bundle.INSTALLED, bundle.getState());
         assertEquals(location, bundle.getLocation());
      }
      finally
      {
         bundle.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundle.getState());
      }
      
      // Test symbolic location
      bundle = context.installBundle("/symbolic/location", url.openStream());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundle.getState());
         assertEquals("/symbolic/location", bundle.getLocation());
      }
      finally
      {
         bundle.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundle.getState());
      }
   }
   
   public void testServiceListener() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         try
         {
            bundleContext.addServiceListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.addServiceListener(null, "(a=b)");
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.removeServiceListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         bundleContext.addServiceListener(this);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, true);
         bundleContext.removeServiceListener(this);
         
         bundleContext.addServiceListener(this);
         bundleContext.removeServiceListener(this);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, false);
         
         bundleContext.addServiceListener(this);
         bundleContext.addServiceListener(this);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, true);
         bundleContext.removeServiceListener(this);
         
         bundleContext.addServiceListener(this, null);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, true);
         bundleContext.removeServiceListener(this);
         
         bundleContext.addServiceListener(this, null);
         bundleContext.removeServiceListener(this);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, false);
         
         bundleContext.addServiceListener(this, null);
         bundleContext.addServiceListener(this, null);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, true);
         bundleContext.removeServiceListener(this);
         
         Dictionary<String, Object> properties = new Hashtable<String, Object>();
         properties.put("a", "b");
         
         bundleContext.addServiceListener(this, ("(a=b)"));
         bundleContext = assertServiceLifecycle(bundle, bundleContext, properties, true);
         bundleContext.removeServiceListener(this);
         
         bundleContext.addServiceListener(this, ("(c=d)"));
         bundleContext = assertServiceLifecycle(bundle, bundleContext, properties, false);
         bundleContext.removeServiceListener(this);
         
         bundleContext.addServiceListener(this, "(a=b)");
         bundleContext.removeServiceListener(this);
         bundleContext = assertServiceLifecycle(bundle, bundleContext, properties, false);
         
         bundleContext.addServiceListener(this, "(c=d)");
         bundleContext.addServiceListener(this, "(a=b)");
         assertServiceLifecycle(bundle, bundleContext, properties, true);
         bundleContext.removeServiceListener(this);
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   protected BundleContext assertServiceLifecycle(Bundle bundle, BundleContext bundleContext, boolean events) throws Exception
   {
      return assertServiceLifecycle(bundle, bundleContext, null, events);
   }
   
   protected BundleContext assertServiceLifecycle(Bundle bundle, BundleContext bundleContext, Dictionary<String, Object> properties, boolean events) throws Exception
   {
      assertNoServiceEvent();
      
      ServiceRegistration registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, properties);
      ServiceReference reference = registration.getReference();
      
      if (events)
         assertServiceEvent(ServiceEvent.REGISTERED, reference);
      else
         assertNoServiceEvent();

      registration.setProperties(properties);
      if (events)
         assertServiceEvent(ServiceEvent.MODIFIED, reference);
      else
         assertNoServiceEvent();

      registration.unregister();
      if (events)
         assertServiceEvent(ServiceEvent.UNREGISTERING, reference);
      else
         assertNoServiceEvent();
      
      registration = bundleContext.registerService(BundleContext.class.getName(), bundleContext, properties);
      reference = registration.getReference();
      if (events)
         assertServiceEvent(ServiceEvent.REGISTERED, reference);
      else
         assertNoServiceEvent();

      bundle.stop();
      if (events)
         assertServiceEvent(ServiceEvent.UNREGISTERING, reference);
      else
         assertNoServiceEvent();
      
      try
      {
         bundleContext.addServiceListener(this);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
      
      bundle.start();
      bundleContext = bundle.getBundleContext();
      assertNotNull(bundleContext);
      return bundleContext;
   }
   
   public void testBundleListener() throws Exception
   {
      // todo how to test INSTALLED/RESOLVED?
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         try
         {
            bundleContext.addBundleListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.removeBundleListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         bundleContext.addBundleListener(this);
         bundleContext = assertBundleLifecycle(bundle, bundleContext, true);
         bundleContext.removeBundleListener(this);
         
         bundleContext.addBundleListener(this);
         bundleContext.removeBundleListener(this);
         bundleContext = assertBundleLifecycle(bundle, bundleContext, false);
         
         bundleContext.addBundleListener(this);
         bundleContext.addBundleListener(this);
         bundleContext = assertBundleLifecycle(bundle, bundleContext, true);
         bundleContext.removeBundleListener(this);

         bundleContext.addBundleListener(this);
         
         // todo test asynch BundleListener
      }
      finally
      {
         uninstall(bundle);
      }
      assertBundleEvent(BundleEvent.STOPPING, bundle);
      assertBundleEvent(BundleEvent.STOPPED, bundle);
      // todo assertBundleEvent(BundleEvent.UNRESOLVED, bundle);
      assertBundleEvent(BundleEvent.UNINSTALLED, bundle);
   }
   
   protected BundleContext assertBundleLifecycle(Bundle bundle, BundleContext bundleContext, boolean events) throws Exception
   {
      assertNoBundleEvent();
      
      bundle.stop();
      if (events)
      {
         assertBundleEvent(BundleEvent.STOPPING, bundle);
         assertBundleEvent(BundleEvent.STOPPED, bundle);
      }
      else
      {
         assertNoBundleEvent();
      }
      
      bundle.start();
      if (events)
      {
         assertBundleEvent(BundleEvent.STARTING, bundle);
         assertBundleEvent(BundleEvent.STARTED, bundle);
      }
      else
      {
         assertNoBundleEvent();
      }
      
      return bundleContext;
   }
   
   public void testFrameworkListener() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         try
         {
            bundleContext.addFrameworkListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext.removeFrameworkListener(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         // todo test events
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testGetDataFile() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         bundle.start();
         BundleContext bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         File dataFile = bundleContext.getDataFile("blah");
         assertNotNull(dataFile);
         assertTrue(dataFile.toString().endsWith(File.separator + "blah"));
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   protected void assertSystemProperty(BundleContext bundleContext, String property, String osgiProperty)
   {
      String expected = System.getProperty(property);
      assertNotNull(expected);
      assertEquals(expected, bundleContext.getProperty(osgiProperty));
   }
}
