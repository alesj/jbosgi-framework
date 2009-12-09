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

import org.osgi.framework.ServiceReference;

/**
 * Service reference based dictionary.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class ServiceRefDictionary extends Dictionary<String, Object>
{
   private ServiceReference ref;

   ServiceRefDictionary(ServiceReference ref)
   {
      if (ref == null)
         throw new IllegalArgumentException("Null ref");
      this.ref = ref;
   }

   public int size()
   {
      String[] keys = ref.getPropertyKeys();
      return (keys != null) ? keys.length : 0;
   }

   public boolean isEmpty()
   {
      return size() == 0;
   }

   public Enumeration<String> keys()
   {
      return new Enumeration<String>()
      {
         String[] keys = ref.getPropertyKeys();
         int index = 0;
         int size = size();

         public boolean hasMoreElements()
         {
            return index < size;
         }

         public String nextElement()
         {
            return keys[index++];
         }
      };
   }

   public Enumeration<Object> elements()
   {
      return new Enumeration<Object>()
      {
         String[] keys = ref.getPropertyKeys();
         int index = 0;
         int size = size();

         public boolean hasMoreElements()
         {
            return index < size;
         }

         public Object nextElement()
         {
            return get(keys[index++]);
         }
      };
   }

   public Object get(Object key)
   {
      if (key == null)
         return null;

      return ref.getProperty(key.toString());
   }

   public Object put(String key, Object value)
   {
      return null;
   }

   public Object remove(Object key)
   {
      return null;
   }
}