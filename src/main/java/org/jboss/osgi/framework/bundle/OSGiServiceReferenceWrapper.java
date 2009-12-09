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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * OSGiServiceReferenceWrapper.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class OSGiServiceReferenceWrapper implements ServiceReference
{
   /** The service state */
   private OSGiServiceState serviceState;

   /**
    * Create a new OSGiServiceReferenceWrapper.
    * 
    * @param serviceState the service state
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiServiceReferenceWrapper(OSGiServiceState serviceState)
   {
      if (serviceState == null)
         throw new IllegalArgumentException("Null service state");
      this.serviceState = serviceState;
   }

   public Bundle getBundle()
   {
      return serviceState.getBundle();
   }

   public Object getProperty(String key)
   {
      return serviceState.getProperty(key);
   }

   public String[] getPropertyKeys()
   {
      return serviceState.getPropertyKeys();
   }

   public Bundle[] getUsingBundles()
   {
      return serviceState.getUsingBundles();
   }

   public boolean isAssignableTo(Bundle bundle, String className)
   {
      return serviceState.isAssignableTo(bundle, className);
   }

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

   /**
    * Get the serviceState.
    * 
    * @return the serviceState.
    */
   OSGiServiceState getServiceState()
   {
      return serviceState;
   }
}
