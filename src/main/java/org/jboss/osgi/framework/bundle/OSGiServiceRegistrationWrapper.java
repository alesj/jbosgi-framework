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

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGiServiceRegistrationWrapper.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class OSGiServiceRegistrationWrapper implements ServiceRegistration
{
   /** The service state */
   private OSGiServiceState serviceState;

   /**
    * Create a new OSGiServiceRegistrationWrapper.
    * 
    * @param serviceState the service state
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiServiceRegistrationWrapper(OSGiServiceState serviceState)
   {
      if (serviceState == null)
         throw new IllegalArgumentException("Null service state");
      this.serviceState = serviceState;
   }

   public ServiceReference getReference()
   {
      return serviceState.getReference();
   }

   @SuppressWarnings("unchecked")
   public void setProperties(Dictionary properties)
   {
      serviceState.setProperties(properties);
   }

   public void unregister()
   {
      serviceState.unregister();
   }

   @Override
   public String toString()
   {
      return serviceState.toString();
   }
}
