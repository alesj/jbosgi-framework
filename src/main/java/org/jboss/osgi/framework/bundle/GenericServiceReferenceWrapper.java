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

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.dependency.spi.tracker.ContextTracking;
import org.osgi.framework.Bundle;

/**
 * GenericServiceReferenceWrapper.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class GenericServiceReferenceWrapper extends ControllerContextHandle
{
   private ControllerContext context;
   private AbstractBundleState bundleState;

   public GenericServiceReferenceWrapper(ControllerContext context, AbstractBundleState bundleState)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      this.context = context;
      this.bundleState = bundleState;
   }

   ControllerContext getContext()
   {
      return context;
   }

   public Object getProperty(String key)
   {
      return MDRUtils.getProperty(context, key, Object.class);
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
      if (OSGiBundleManager.isUnregistered(context))
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
         Set<Object> users = ct.getUsers(context);
         Set<Bundle> bundles = new HashSet<Bundle>();
         for (Object user : users)
         {
            AbstractBundleState abs = manager.getBundleForUser(user);
            bundles.add(abs.getBundleInternal());
         }
         if (bundles.isEmpty() == false)
            return bundles.toArray(new Bundle[bundles.size()]);
      }
      return null;
   }

   public boolean isAssignableTo(Bundle bundle, String className)
   {
      return MDRUtils.isAssignableTo(context, bundleState, bundle, className); 
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