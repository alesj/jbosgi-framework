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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.jboss.test.kernel.junit.MicrocontainerTest;
import org.jboss.virtual.AssembledDirectory;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;

/**
 * OSGiTestCase - Parent Test Case for OSGi tests.  
 * 
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 * @version $Revision: 87848 $
 */
public abstract class FrameworkTest extends MicrocontainerTest implements FrameworkListener, SynchronousBundleListener, ServiceListener
{
   private final List<FrameworkEvent> frameworkEvents = new CopyOnWriteArrayList<FrameworkEvent>();
   private final List<BundleEvent> bundleEvents = new CopyOnWriteArrayList<BundleEvent>();
   private final List<ServiceEvent> serviceEvents = new CopyOnWriteArrayList<ServiceEvent>();

   /**
    * Create a new OSGiTestCase.
    * 
    * @param name the test name
    */
   public FrameworkTest(String name)
   {
      super(name);
   }

   public static FrameworkTestDelegate getDelegate(Class<?> clazz) throws Exception
   {
      return new FrameworkTestDelegate(clazz);
   }

   /**
    * Get OSGiTestDelegate
    */
   protected FrameworkTestDelegate getDelegate()
   {
      return (FrameworkTestDelegate)super.getDelegate();
   }

   /**
    * Get BundleManager from Delegate
    * 
    * @return The BundleManager
    */
   protected OSGiBundleManager getBundleManager()
   {
      return getDelegate().getBundleManager();
   }

   /**
    * Get the system bundle
    * 
    * @return the system bundle
    */
   protected Bundle getSystemBundle()
   {
      return getBundleManager().getBundleById(0).getBundleInternal();
   }

   /**
    * Add the base bundles
    * 
    * @param expected the collection to add to
    */
   protected void addBaseBundles(Collection<Bundle> expected)
   {
      expected.add(getSystemBundle());
   }

   /**
    * Create a bundle 
    * 
    * @param root the location of the location to deploy
    * @param child the child to deploy
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   protected Bundle addBundle(String root, String child) throws Exception
   {
      return getDelegate().addBundle(root, child);
   }

   /**
    * Create a bundle 
    *
    * @param file the virtual file
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   protected Bundle addBundle(VirtualFile file) throws Exception
   {
      return getDelegate().addBundle(file);
   }

   /**
    * Create a bundle 
    * 
    * @param root the location of the location to deploy
    * @param child the child to deploy
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   protected Bundle deployBundle(String root, String child) throws Exception
   {
      return getDelegate().deployBundle(root, child);
   }

   /**
    * Create a bundle 
    *
    * @param file the virtual file
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   protected Bundle deployBundle(VirtualFile file) throws Exception
   {
      return getDelegate().deployBundle(file);
   }

   /**
    * Remove a bundle 
    * 
    * @param bundle the bundle to remove
    * @throws Exception for any error
    */
   protected void uninstall(Bundle bundle) throws Exception
   {
      getDelegate().uninstall(bundle);
   }

   protected DeploymentUnit getDeploymentUnit(Bundle bundle)
   {
      return getDelegate().getDeploymentUnit(bundle);
   }

   protected Bundle assembleBundle(String name, String resources, Class<?>... packages) throws Exception
   {
      return getDelegate().assembleBundle(name, new String[] { resources }, packages);
   }

   protected Bundle assembleBundle(String name, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      return getDelegate().assembleBundle(name, resourcePaths, packages);
   }

   protected Bundle deployBundle(String name, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, null, (String[]) null, packages);
   }

   protected Bundle deployBundle(String name, String resourcePath, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, null, resourcePath, packages);
   }

   protected Bundle deployBundle(String name, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, null, resourcePaths, packages);
   }

   protected Bundle deployBundle(String name, OSGiMetaData metaData, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, metaData, (String[]) null, packages);
   }

   protected Bundle deployBundle(String name, OSGiMetaData metaData, String resourcePath, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, metaData, resourcePath, packages);
   }

   protected Bundle deployBundle(String name, OSGiMetaData metaData, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      return getDelegate().deployBundle(name, metaData, resourcePaths, packages);
   }

   protected AssembledDirectory createAssembledDirectory(String name) throws Exception
   {
      return getDelegate().createAssembledDirectory(name, "");
   }

   protected AssembledDirectory createAssembledDirectory(String name, String rootName) throws Exception
   {
      return getDelegate().createAssembledDirectory(name, rootName);
   }

   protected void addPackage(AssembledDirectory dir, Class<?> reference) throws Exception
   {
      getDelegate().addPackage(dir, reference);
   }

   protected void addPath(final AssembledDirectory dir, String path, String name) throws Exception
   {
      getDelegate().addPath(dir, path, name);
   }

   protected void assertClassEquality(Class<?> expected, Class<?> actual)
   {
      assertTrue("Should be the same " + ClassLoaderUtils.classToString(expected) + " and " + ClassLoaderUtils.classToString(actual), expected == actual);
   }

   protected void assertNoClassEquality(Class<?> expected, Class<?> actual)
   {
      assertTrue("Should NOT be the same " + ClassLoaderUtils.classToString(expected) + " and " + ClassLoaderUtils.classToString(actual), expected != actual);
   }

   protected void assertClassLoader(Class<?> clazz, Bundle expected)
   {
      if (expected == null)
         return;
      ClassLoader cl = clazz.getClassLoader();
      ClassLoader bundleClassLoader = getBundleClassLoader(expected);
      boolean result = bundleClassLoader.equals(cl);
      assertTrue(ClassLoaderUtils.classToString(clazz) + " should have expected classloader=" + expected, result);
   }

   protected ClassLoader getBundleClassLoader(Bundle expected)
   {
      return getDeploymentUnit(expected).getClassLoader();
   }

   protected Class<?> assertLoadClass(Bundle start, Class<?> reference) throws Exception
   {
      return assertLoadClass(start, reference, start);
   }

   protected Class<?> assertLoadClass(Bundle start, Class<?> reference, Bundle expected) throws Exception
   {
      return assertLoadClass(start, reference, expected, false);
   }

   protected Class<?> assertLoadClass(Bundle start, Class<?> reference, Bundle expected, boolean isReference) throws Exception
   {
      Class<?> result = assertLoadClass(reference.getName(), start, expected);
      if (isReference)
         assertClassEquality(reference, result);
      else
         assertNoClassEquality(reference, result);
      return result;
   }

   protected Class<?> assertLoadClass(String name, Bundle bundle, Bundle expected)
   {
      Class<?> result = null;
      try
      {
         result = bundle.loadClass(name);
         getLog().debug("Got class: " + ClassLoaderUtils.classToString(result) + " for " + name + " from " + bundle);
      }
      catch (ClassNotFoundException e)
      {
         failure("Did not expect CNFE for " + name + " from " + bundle, e);
      }
      assertClassLoader(result, expected);
      return result;
   }

   protected void assertLoadClassFail(Bundle start, Class<?> reference)
   {
      assertLoadClassFail(start, reference.getName());
   }

   protected void assertLoadClassFail(Bundle start, String name)
   {
      try
      {
         start.loadClass(name);
         fail("Should not be here!");
      }
      catch (Exception expected)
      {
         checkThrowable(ClassNotFoundException.class, expected);
      }
   }

   protected URL getBundleResource(Bundle bundle, String path)
   {
      return getDelegate().getBundleResource(bundle, path);
   }

   protected Enumeration<URL> getBundleResources(Bundle bundle, String path) throws Exception
   {
      return getDelegate().getBundleResources(bundle, path);
   }

   protected void assertNoAllReferences(BundleContext bundleContext, String clazz) throws Exception
   {
      assertNoAllReferences(bundleContext, clazz, null);
   }

   protected void assertNoAllReferences(BundleContext bundleContext, String clazz, String filter) throws Exception
   {
      ServiceReference[] actual = bundleContext.getAllServiceReferences(clazz, filter);
      if (actual != null)
         getLog().debug(bundleContext + " got " + Arrays.asList(actual) + " for clazz=" + clazz + " filter=" + filter);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz + " filter=" + filter);
      assertNull("Expected no references for clazz=" + clazz + " filter=" + filter, actual);
   }

   protected void assertAllReferences(BundleContext bundleContext, String clazz, ServiceReference... expected) throws Exception
   {
      assertAllReferences(bundleContext, clazz, null, expected);
   }

   protected void assertAllReferences(BundleContext bundleContext, String clazz, String filter, ServiceReference... expected) throws Exception
   {
      ServiceReference[] actual = bundleContext.getAllServiceReferences(clazz, filter);
      if (actual != null)
         getLog().debug(bundleContext + " got " + Arrays.asList(actual) + " for clazz=" + clazz + " filter=" + filter);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz + " filter=" + filter);
      assertEquals(bundleContext + " with clazz=" + clazz + " filter=" + filter, expected, actual);
   }

   protected void assertNoReferences(BundleContext bundleContext, String clazz) throws Exception
   {
      assertNoReferences(bundleContext, clazz, null);
   }

   protected void assertNoReferences(BundleContext bundleContext, String clazz, String filter) throws Exception
   {
      ServiceReference[] actual = bundleContext.getServiceReferences(clazz, filter);
      if (actual != null)
         getLog().debug(bundleContext + " got " + Arrays.asList(actual) + " for clazz=" + clazz + " filter=" + filter);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz + " filter=" + filter);
      assertNull("Expected no references for clazz=" + clazz + " filter=" + filter, actual);
   }

   protected void assertReferences(BundleContext bundleContext, String clazz, ServiceReference... expected) throws Exception
   {
      assertReferences(bundleContext, clazz, null, expected);
   }

   protected void assertReferences(BundleContext bundleContext, String clazz, String filter, ServiceReference... expected) throws Exception
   {
      ServiceReference[] actual = bundleContext.getServiceReferences(clazz, filter);
      if (actual != null)
         getLog().debug(bundleContext + " got " + Arrays.asList(actual) + " for clazz=" + clazz + " filter=" + filter);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz + " filter=" + filter);
      assertEquals(bundleContext + " with clazz=" + clazz + " filter=" + filter, expected, actual);
   }

   protected void assertNoGetReference(BundleContext bundleContext, String clazz) throws Exception
   {
      ServiceReference actual = bundleContext.getServiceReference(clazz);
      if (actual != null)
         getLog().debug(bundleContext + " got " + actual + " for clazz=" + clazz);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz);
      assertNull("Expected no references for clazz=" + clazz, actual);
   }

   protected void assertGetReference(BundleContext bundleContext, String clazz, ServiceReference expected) throws Exception
   {
      ServiceReference actual = bundleContext.getServiceReference(clazz);
      if (actual != null)
         getLog().debug(bundleContext + " got " + Arrays.asList(actual) + " for clazz=" + clazz);
      else
         getLog().debug(bundleContext + " got nothing for clazz=" + clazz);
      assertEquals(bundleContext + " with clazz=" + clazz, expected, actual);
   }

   protected void assertUsingBundles(ServiceReference reference, Bundle... bundles)
   {
      Set<Bundle> expected = new HashSet<Bundle>();
      expected.addAll(Arrays.asList(bundles));

      Set<Bundle> actual = new HashSet<Bundle>();
      Bundle[] users = reference.getUsingBundles();
      if (users != null)
         actual.addAll(Arrays.asList(users));

      getLog().debug(reference + " users=" + actual);

      assertEquals(expected, actual);
   }

   protected void assertObjectClass(String expected, ServiceReference reference)
   {
      assertObjectClass(new String[] { expected }, reference);
   }

   protected void assertObjectClass(String[] expected, ServiceReference reference)
   {
      Object actual = reference.getProperty(Constants.OBJECTCLASS);
      if (actual == null)
         fail("no object class???");
      if (actual instanceof String[] == false)
         fail(actual + " is not a string array??? " + actual.getClass().getName());
      assertEquals(expected, (String[])actual);
   }

   public void frameworkEvent(FrameworkEvent event)
   {
      synchronized (frameworkEvents)
      {
         getLog().debug("FrameworkEvent type=" + ConstantsHelper.frameworkEvent(event.getType()) + " for " + event);
         frameworkEvents.add(event);
         frameworkEvents.notifyAll();
      }
   }

   protected void assertNoFrameworkEvent() throws Exception
   {
      getLog().debug("frameworkEvents=" + frameworkEvents);
      assertEquals(0, frameworkEvents.size());
   }

   protected void assertFrameworkEvent(int type) throws Exception
   {
      assertFrameworkEvent(type, getSystemBundle(), null);
   }

   protected void assertFrameworkEvent(int type, Class<? extends Throwable> expectedThrowable) throws Exception
   {
      assertFrameworkEvent(type, getSystemBundle(), expectedThrowable);
   }

   protected void assertFrameworkEvent(int type, Bundle bundle, Class<? extends Throwable> expectedThrowable) throws Exception
   {
      waitForEvent(frameworkEvents, type);
      getLog().debug("frameworkEvents=" + frameworkEvents);
      int size = frameworkEvents.size();
      assertTrue("" + size, size > 0);
      FrameworkEvent event = frameworkEvents.remove(0);
      assertEquals(ConstantsHelper.frameworkEvent(type), ConstantsHelper.frameworkEvent(event.getType()));
      Throwable t = event.getThrowable();
      if (expectedThrowable == null)
      {
         if (t != null)
         {
            getLog().error("Unexpected error in Framework event: ", t);
            fail("Unexpected throwable: " + t);
         }
      }
      else
      {
         checkThrowable(BundleException.class, t);
         checkDeepThrowable(expectedThrowable, t);
      }
      assertEquals(bundle, event.getSource());
      assertEquals(bundle, event.getBundle());
   }

   public void bundleChanged(BundleEvent event)
   {
      synchronized (bundleEvents)
      {
         getLog().debug("BundleChanged type=" + ConstantsHelper.bundleEvent(event.getType()) + " for " + event);
         bundleEvents.add(event);
         bundleEvents.notifyAll();
      }
   }

   protected void assertNoBundleEvent() throws Exception
   {
      getLog().debug("bundleEvents=" + bundleEvents);
      assertEquals(0, bundleEvents.size());
   }

   protected void assertBundleEvent(int type, Bundle bundle) throws Exception
   {
      waitForEvent(bundleEvents, type);
      
      getLog().debug("bundleEvents=" + bundleEvents);
      int size = bundleEvents.size();
      assertTrue("" + size, size > 0);
      
      if (bundle instanceof AbstractBundleState)
         bundle = ((AbstractBundleState)bundle).getBundle();
      
      BundleEvent foundEvent = null;
      for(int i=0; i < bundleEvents.size(); i++)
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

   public void serviceChanged(ServiceEvent event)
   {
      synchronized (serviceEvents)
      {
         getLog().debug("ServiceChanged type=" + ConstantsHelper.serviceEvent(event.getType()) + " for " + event);
         serviceEvents.add(event);
         serviceEvents.notifyAll();
      }
   }

   protected void assertNoServiceEvent() throws Exception
   {
      getLog().debug("serviceEvents=" + serviceEvents);
      assertEquals(0, serviceEvents.size());
   }

   protected void assertServiceEvent(int type, ServiceReference reference) throws Exception
   {
      waitForEvent(serviceEvents, type);
      getLog().debug("serviceEvents=" + serviceEvents);
      int size = serviceEvents.size();
      assertTrue("" + size, size > 0);
      ServiceEvent event = serviceEvents.remove(0);
      assertEquals(ConstantsHelper.serviceEvent(type), ConstantsHelper.serviceEvent(event.getType()));
      assertEquals(reference, event.getSource());
      assertEquals(reference, event.getServiceReference());
   }

   @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
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
