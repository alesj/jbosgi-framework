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
import java.util.Set;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.osgi.framework.metadata.ServiceControllerContext;
import org.jboss.osgi.framework.metadata.ServiceMetaData;
import org.osgi.framework.Constants;

/**
 * JMX dictionary factory.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class JMXDictionaryFactory extends AbstractDictionaryFactory<ServiceControllerContext>
{
   public JMXDictionaryFactory(KernelConfigurator configurator)
   {
      super(configurator);
   }

   public Class<ServiceControllerContext> getContextType()
   {
      return ServiceControllerContext.class;
   }

   public Dictionary<String, Object> getDictionary(ServiceControllerContext context)
   {
      return new JMXDictionary(context);
   }

   private class JMXDictionary extends GenericDictionary
   {
      protected JMXDictionary(ServiceControllerContext context)
      {
         super(context);
      }

      @Override
      protected String[] getClasses(ControllerContext context)
      {
         ServiceControllerContext scc = ServiceControllerContext.class.cast(context);
         ServiceMetaData smd = scc.getServiceMetaData();
         if (smd != null)
         {
            Set<String> interfaces = smd.getInterfaces();
            if (interfaces != null)
            {
               String[] result = new String[interfaces.size()];
               interfaces.toArray(result);
               put(Constants.OBJECTCLASS, result);
               return result;
            }
         }
         return super.getClasses(context);
      }
   }
}