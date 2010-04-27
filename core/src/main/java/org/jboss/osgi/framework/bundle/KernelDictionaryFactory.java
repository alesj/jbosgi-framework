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

import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.RelatedClassMetaData;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.reflect.spi.ClassInfo;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

/**
 * Kernel dictionary factory.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class KernelDictionaryFactory extends AbstractDictionaryFactory<KernelControllerContext>
{
   public KernelDictionaryFactory(KernelConfigurator configurator)
   {
      super(configurator);
   }

   public Class<KernelControllerContext> getContextType()
   {
      return KernelControllerContext.class;
   }

   public Dictionary<String, Object> getDictionary(KernelControllerContext context)
   {
      return new KernelDictionary(context);
   }

   private class KernelDictionary extends GenericDictionary
   {
      private KernelDictionary(KernelControllerContext context)
      {
         super(context);
      }

      @Override
      protected String[] getClasses(ControllerContext context)
      {
         KernelControllerContext kcc = KernelControllerContext.class.cast(context);
         BeanMetaData bmd = kcc.getBeanMetaData();
         if (bmd != null)
         {
            Set<RelatedClassMetaData> rcmds = bmd.getRelated();
            if (rcmds != null && rcmds.isEmpty() == false)
            {
               Set<String> classes = new HashSet<String>();
               for (RelatedClassMetaData rcmd : rcmds)
               {
                  if ("OSGi".equalsIgnoreCase(rcmd.getEnabled(String.class)))
                  {
                     classes.add(rcmd.getClassName());
                  }
               }
               if (classes.isEmpty() == false)
               {
                  String[] result = classes.toArray(new String[classes.size()]);
                  put(Constants.OBJECTCLASS, result);
                  return result;
               }
            }
         }
         return super.getClasses(context);
      }

      @Override
      protected ClassInfo getFromNullTarget(ControllerContext context)
      {
         KernelControllerContext kcc = KernelControllerContext.class.cast(context);
         BeanInfo beanInfo = kcc.getBeanInfo();
         return beanInfo != null ? beanInfo.getClassInfo() : null;
      }
   }
}