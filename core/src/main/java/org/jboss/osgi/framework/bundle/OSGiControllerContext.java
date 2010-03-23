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
import java.util.Set;

import org.jboss.dependency.plugins.AbstractControllerContext;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.util.collection.CollectionsFactory;
import org.jboss.util.id.GUID;

/**
 * OSGiServiceState.
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Mar-2010
 */
public class OSGiControllerContext extends AbstractControllerContext
{
   /** The alias constant */
   private static final String SERVICE_ALIAS = "service.alias";

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public OSGiControllerContext(Object service, Dictionary properties)
   {
      super(GUID.asString(), getAlias(properties), OSGiControllerContextActions.ACTIONS, null, service);
   }

   public Object getTarget(ContextTracker tracker)
   {
      // This changes the order of functionality to: 
      //
      // #1 incrementing usage count
      // #2 obtain the result
      //
      // The reason is that the OSGi {@link ServiceFactory} getService() method
      // is called with a {@link ServiceReference} that expects the given tracker 
      // (i.e. the using bundle) already be included in getUsingBundles.  

      if (tracker != null)
      {
         ContextTracker myTracker = getContextTracker();
         if (myTracker != null && myTracker != tracker)
            myTracker.incrementUsedBy(this, tracker);

         tracker.incrementUsedBy(this, tracker);
      }

      // Get the service object
      Object result = getTargetForActualUser(tracker);
      
      // In case the ServiceFactory.getService() returns null decrement the usage count again
      if (result == null)
      {
         ContextTracker myTracker = getContextTracker();
         if (myTracker != null && myTracker != tracker)
            myTracker.decrementUsedBy(this, tracker);

         tracker.decrementUsedBy(this, tracker);
      }

      return result;
   }

   public Object ungetTarget(ContextTracker tracker)
   {
      // This changes the order of functionality to: 
      //
      // #1 obtain the result
      // #2 decrementing usage count
      //
      // The reason is that the OSGi {@link ServiceFactory} ungetService() method
      // is called with a {@link ServiceReference} that expects the given tracker 
      // (i.e. the using bundle) already be included in getUsingBundles.

      Object result = ungetTargetForActualUser(tracker);
      if (tracker != null)
      {
         ContextTracker myTracker = getContextTracker();
         if (myTracker != null && myTracker != tracker)
            myTracker.decrementUsedBy(this, tracker);

         tracker.decrementUsedBy(this, tracker);
      }
      return result;
   }

   // Check if there is an alias in properties.
   private static Set<Object> getAlias(Dictionary<String, Object> properties)
   {
      if (properties != null)
      {
         Set<Object> aliases = null;
         Enumeration<String> keys = properties.keys();
         while (keys.hasMoreElements())
         {
            String key = keys.nextElement();
            if (key.startsWith(SERVICE_ALIAS))
            {
               if (aliases == null)
                  aliases = CollectionsFactory.createLazySet();

               Object alias = properties.get(key);
               aliases.add(alias);
            }
         }
         return aliases;
      }
      return null;
   }
}
