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

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.reflect.spi.ClassInfo;
import org.jboss.util.collection.Iterators;
import org.osgi.framework.Constants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract dictionary factory.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractDictionaryFactory<T extends ControllerContext> implements DictionaryFactory<T>
{
   protected static final String NAME = "bean.name";
   protected static final String[] EMPTY = new String[0];
   private KernelConfigurator configurator;
   private final ClassInfo OBJECT;

   public AbstractDictionaryFactory(KernelConfigurator configurator)
   {
      if (configurator == null)
         throw new IllegalArgumentException("Null configurator");

      this.configurator = configurator;
      OBJECT = getClassInfo(Object.class);
   }

   protected ClassInfo getClassInfo(Class<?> clazz)
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

   protected abstract class AbstractDictionary extends Dictionary<String, Object>
   {
      private Map<Object, Object> map;
      private ControllerContext  context;

      protected AbstractDictionary(ControllerContext  context)
      {
         this.context = context;
         this.map = new ConcurrentHashMap<Object, Object>(2);
         map.put(NAME, getName(context));
         map.put(Constants.OBJECTCLASS, EMPTY);
      }

      protected Object getName(ControllerContext context)
      {
         return context.getName();
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

         return getClasses(context);
      }

      protected abstract String[] getClasses(ControllerContext context);

      public Object put(String key, Object value)
      {
         return map.put(key, value);
      }

      public Object remove(Object key)
      {
         return map.remove(key);
      }
   }

   // this one exposes whole target type/class hierarchy
   protected class GenericDictionary extends AbstractDictionary
   {
      protected GenericDictionary(ControllerContext context)
      {
         super(context);
      }

      @Override
      protected String[] getClasses(ControllerContext context)
      {
         ClassInfo clazz;
         Object target = context.getTarget();
         if (target != null)
         {
            clazz = getClassInfo(target.getClass());
         }
         else
            clazz = getFromNullTarget(context);

         String[] classes = EMPTY;
         if (clazz != null)
         {
            Set<String> clazzes = new HashSet<String>();
            traverseClass(clazz, clazzes);
            classes = clazzes.toArray(new String[clazzes.size()]);
            put(Constants.OBJECTCLASS, classes);
         }
         return classes;
      }

      protected ClassInfo getFromNullTarget(ControllerContext context)
      {
         return null;
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