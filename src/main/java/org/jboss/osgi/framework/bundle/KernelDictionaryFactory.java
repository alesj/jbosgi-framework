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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.reflect.spi.ClassInfo;
import org.jboss.util.collection.Iterators;
import org.osgi.framework.Constants;

/**
 * Kernel dictionary factory.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDictionaryFactory implements DictionaryFactory<KernelControllerContext>
{
   private static final String NAME = "bean.name";
   private static final String[] EMPTY = new String[0];
   private KernelConfigurator configurator;
   private final ClassInfo OBJECT;

   public KernelDictionaryFactory(KernelConfigurator configurator)
   {
      if (configurator == null)
         throw new IllegalArgumentException("Null configurator");

      this.configurator = configurator;
      OBJECT = getClassInfo(Object.class);
   }

   private ClassInfo getClassInfo(Class<?> clazz)
   {
      try
      {
         return configurator.getClassInfo(clazz);
      }
      catch (Throwable t)
      {
         throw new RuntimeException(t);
      }
   }

   public Class<KernelControllerContext> getContextType()
   {
      return KernelControllerContext.class;
   }

   public Dictionary<String, Object> getDictionary(KernelControllerContext context)
   {
      return new KernelDictionary(context);
   }

   private class KernelDictionary extends Dictionary<String, Object>
   {
      private Map<Object, Object> map;
      private KernelControllerContext context;

      private KernelDictionary(KernelControllerContext context)
      {
         this.context = context;
         this.map = new ConcurrentHashMap<Object, Object>(2);
         map.put(NAME, context.getName());
         map.put(Constants.OBJECTCLASS, EMPTY);
      }

      public int size()
      {
         return map.size();
      }

      public boolean isEmpty()
      {
         return size() == 0;
      }

      @SuppressWarnings({"unchecked"})
      public Enumeration<String> keys()
      {
         return Iterators.toEnumeration(map.keySet().iterator());
      }

      @SuppressWarnings({"unchecked"})
      public Enumeration<Object> elements()
      {
         return Iterators.toEnumeration(map.values().iterator());
      }

      public Object get(Object key)
      {
         Object value = map.get(key);
         if (value != EMPTY)
            return value;

         ClassInfo clazz = null;
         Object target = context.getTarget();
         BeanInfo info = context.getBeanInfo();
         if (target != null)
         {
            clazz = getClassInfo(target.getClass());
         }
         else if (info != null)
         {
            clazz = info.getClassInfo();
         }

         String[] classes = EMPTY;
         if (clazz != null)
         {
            Set<String> clazzes = new HashSet<String>();
            traverseClass(clazz, clazzes);
            classes = clazzes.toArray(new String[clazzes.size()]);
            map.put(Constants.OBJECTCLASS, classes);
         }
         return classes;
      }

      public Object put(String key, Object value)
      {
         return map.put(key, value);
      }

      public Object remove(Object key)
      {
         return map.remove(key);
      }

      protected void traverseClass(ClassInfo clazz, Set<String> classes)
      {
         if (clazz == null || clazz == OBJECT)
         {
            return;
         }

         classes.add(clazz.getName());

         // traverse superclass
         traverseClass(clazz.getSuperclass(), classes);
         ClassInfo[] interfaces = clazz.getInterfaces();
         if (interfaces != null)
         {
            // traverse interfaces
            for(ClassInfo intface : interfaces)
            {
               traverseClass(intface, classes);
            }
         }
      }
   }
}