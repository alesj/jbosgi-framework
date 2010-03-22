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

import org.jboss.dependency.spi.ControllerContext;
import org.osgi.framework.Bundle;

/**
 * OSGiServiceReferenceWrapper.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
class OSGiServiceReferenceWrapper extends ControllerContextHandle
{
   /** The service state */
   private OSGiServiceState serviceState;

   OSGiServiceReferenceWrapper(OSGiServiceState serviceState)
   {
      if (serviceState == null)
         throw new IllegalArgumentException("Null service state");
      this.serviceState = serviceState;
   }

   @Override
   public Bundle getBundle()
   {
      return serviceState.getBundle();
   }

   @Override
   public Object getProperty(String key)
   {
      return serviceState.getProperty(key);
   }

   @Override
   public String[] getPropertyKeys()
   {
      return serviceState.getPropertyKeys();
   }

   @Override
   public Bundle[] getUsingBundles()
   {
      return serviceState.getUsingBundles();
   }

   @Override
   public boolean isAssignableTo(Bundle bundle, String className)
   {
      return serviceState.isAssignableTo(bundle, className);
   }

   OSGiServiceState getServiceState()
   {
      return serviceState;
   }

   @Override
   ControllerContext getContext()
   {
      return getServiceState();
   }

   @Override
   public int compareTo(Object reference)
   {
      return serviceState.compareTo(reference);
   }

   @Override
   public boolean equals(Object obj)
   {
      return serviceState.equals(obj);
   }

   @Override
   public int hashCode()
   {
      return serviceState.hashCode();
   }

   @Override
   public String toString()
   {
      return serviceState.toString();
   }
}
