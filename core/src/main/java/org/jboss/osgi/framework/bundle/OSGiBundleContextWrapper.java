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
package org.jboss.osgi.framework.bundle;

// $Id: $

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A wrapper around the bundle state that just exposes the BundleContext API.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleContextWrapper implements BundleContext
{
   /** The bundle state */
   private AbstractBundleState bundleState;
   /** The bundles canonical name */
   private String canonicalName;
   
   /**
    * Create a new OSGiBundleContextWrapper.
    * 
    * @param bundleState the bundle state
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleContextWrapper(AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");
      
      this.bundleState = bundleState;
      this.canonicalName = bundleState.getCanonicalName();
   }

   void destroyBundleContext()
   {
      bundleState = null;
   }
   
   public void addBundleListener(BundleListener listener)
   {
      checkValidBundleContext();
      bundleState.addBundleListener(listener);
   }

   public void addFrameworkListener(FrameworkListener listener)
   {
      checkValidBundleContext();
      bundleState.addFrameworkListener(listener);
   }

   public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      bundleState.addServiceListener(listener, filter);
   }

   public void addServiceListener(ServiceListener listener)
   {
      checkValidBundleContext();
      bundleState.addServiceListener(listener);
   }

   public Filter createFilter(String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return bundleState.createFilter(filter);
   }

   public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return bundleState.getAllServiceReferences(clazz, filter);
   }

   public Bundle getBundle()
   {
      checkValidBundleContext();
      return bundleState.getBundle();
   }

   public Bundle getBundle(long id)
   {
      checkValidBundleContext();
      return bundleState.getBundle(id);
   }

   public Bundle[] getBundles()
   {
      checkValidBundleContext();
      return bundleState.getBundles();
   }

   public File getDataFile(String filename)
   {
      checkValidBundleContext();
      return bundleState.getDataFile(filename);
   }

   public String getProperty(String key)
   {
      checkValidBundleContext();
      return bundleState.getProperty(key);
   }

   public Object getService(ServiceReference reference)
   {
      checkValidBundleContext();
      return bundleState.getService(reference);
   }

   public ServiceReference getServiceReference(String clazz)
   {
      checkValidBundleContext();
      return bundleState.getServiceReference(clazz);
   }

   public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return bundleState.getServiceReferences(clazz, filter);
   }

   public Bundle installBundle(String location, InputStream input) throws BundleException
   {
      checkValidBundleContext();
      return bundleState.installBundle(location, input);
   }

   public Bundle installBundle(String location) throws BundleException
   {
      checkValidBundleContext();
      return bundleState.installBundle(location);
   }

   public Bundle install(VirtualFile root) throws BundleException
   {
      checkValidBundleContext();
      return bundleState.installBundle(root);
   }
   
   @SuppressWarnings("rawtypes")
   public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
   {
      checkValidBundleContext();
      return bundleState.registerService(clazz, service, properties);
   }

   @SuppressWarnings("rawtypes")
   public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
   {
      checkValidBundleContext();
      return bundleState.registerService(clazzes, service, properties);
   }

   public void removeBundleListener(BundleListener listener)
   {
      checkValidBundleContext();
      bundleState.removeBundleListener(listener);
   }

   public void removeFrameworkListener(FrameworkListener listener)
   {
      checkValidBundleContext();
      bundleState.removeFrameworkListener(listener);
   }

   public void removeServiceListener(ServiceListener listener)
   {
      checkValidBundleContext();
      bundleState.removeServiceListener(listener);
   }

   public boolean ungetService(ServiceReference reference)
   {
      checkValidBundleContext();
      return bundleState.ungetService(reference);
   }

   @Override
   public String toString()
   {
      return canonicalName;
   }

   /**
    * Check a bundle context is still valid
    * @throws IllegalStateException when the context is no longer valid
    */
   protected synchronized void checkValidBundleContext()
   {
      if (bundleState == null)
         throw new IllegalStateException("Invalid bundle context: " + canonicalName);
   }
}
