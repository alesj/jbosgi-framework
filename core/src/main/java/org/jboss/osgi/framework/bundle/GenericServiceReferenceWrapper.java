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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.dependency.spi.tracker.ContextTracking;
import org.jboss.kernel.plugins.dependency.AbstractKernelControllerContext;
import org.jboss.osgi.framework.plugins.ControllerContextPlugin;
import org.jboss.osgi.framework.util.KernelUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * A ServiceReference implementation that is backed by a non OSGi 
 * {@link ControllerContext}. This can be an arbitrary MC bean.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 */
class GenericServiceReferenceWrapper extends ControllerContextHandle implements ServiceReference
{
   private AbstractKernelControllerContext context;
   private AbstractBundleState bundleState;
   private Long serviceId;

   public GenericServiceReferenceWrapper(AbstractKernelControllerContext context, AbstractBundleState bundleState)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      this.context = context;
      this.bundleState = bundleState;

      // The Framework adds the following service properties to the service properties
      // * A property named Constants.SERVICE_ID identifying the registration number of the service
      // * A property named Constants.OBJECTCLASS containing all the specified classes.
      
      // [TODO ServiceMix] Revisit generic service.id. The service.id influences the 
      // service order and should not be generated lazily
      serviceId = MDRUtils.getId(context);
      if (serviceId == null)
         serviceId = OSGiServiceState.getNextServiceId();
   }

   ControllerContext getContext()
   {
      return context;
   }

   public Object getProperty(String key)
   {
      if (key == null)
         throw new IllegalArgumentException("Null property key");

      Object value;
      
      // [TODO ServiceMix] getProperty(Constants.SERVICE_ID)
      if (Constants.SERVICE_ID.equals(key))
      {
         value = serviceId;
      }

      // [TODO ServiceMix] getProperty(Constants.OBJECTCLASS)
      if (Constants.OBJECTCLASS.equals(key))
      {
         value = MDRUtils.getClasses(context);
         
         if (value == null)
         {
            BeanMetaData bmd = context.getBeanMetaData();
            String objectClass = bmd.getBean();
            value = new String[] { objectClass };
         }
      }
      else
      {
         value = MDRUtils.getProperty(context, key, Object.class);
      }

      return value;
   }

   public String[] getPropertyKeys()
   {
      Dictionary<String, Object> dictionary = MDRUtils.getProperties(context);
      String[] keys = new String[dictionary.size()];
      int i = 0;
      Enumeration<String> e = dictionary.keys();
      while (e.hasMoreElements())
         keys[i++] = e.nextElement();
      return keys;
   }

   public Bundle getBundle()
   {
      if (KernelUtils.isUnregistered(context))
         return null;

      return bundleState.getBundleInternal();
   }

   public Bundle[] getUsingBundles()
   {
      if (context instanceof ContextTracking)
      {
         ContextTracking tracking = (ContextTracking)context;
         ContextTracker ct = tracking.getContextTracker();
         if (ct == null)
            return null;

         OSGiBundleManager manager = bundleState.getBundleManager();
         ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);

         Set<Object> users = ct.getUsers(context);
         Set<Bundle> bundles = new HashSet<Bundle>();
         for (Object user : users)
         {
            AbstractBundleState abs = plugin.getBundleForUser(user);
            bundles.add(abs.getBundleInternal());
         }
         if (bundles.isEmpty() == false)
            return bundles.toArray(new Bundle[bundles.size()]);
      }
      return null;
   }

   public boolean isAssignableTo(Bundle bundle, String className)
   {
      AbstractBundleState targetBundle = AbstractBundleState.assertBundleState(bundle);
      return MDRUtils.isAssignableTo(context, bundleState, targetBundle, className);
   }

   public int compareTo(Object obj)
   {
      return MDRUtils.compareTo(context, obj);
   }

   public int hashCode()
   {
      return context.hashCode();
   }

   public boolean equals(Object obj)
   {
      if (obj instanceof GenericServiceReferenceWrapper == false)
         return false;

      GenericServiceReferenceWrapper other = (GenericServiceReferenceWrapper)obj;
      return context == other.context;
   }

   public String toString()
   {
      return context.toString();
   }
}