/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.osgi.service.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LazyBundle
{
   public static Bundle getBundle(DeploymentUnit unit) throws Exception
   {
      return (Bundle)Proxy.newProxyInstance(Bundle.class.getClassLoader(), new Class<?>[] { Bundle.class }, new LazyBundleHandler(unit));
   }

   private static class LazyBundleHandler implements InvocationHandler
   {
      private DeploymentUnit unit;
      private Bundle bundle;

      private LazyBundleHandler(DeploymentUnit unit)
      {
         this.unit = unit;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         return method.invoke(getBundle(), args);
      }

      private Bundle getBundle()
      {
         if (bundle == null)
         {
            AbstractBundleState bundle = unit.getAttachment(AbstractBundleState.class);
            if (bundle == null)
               throw new IllegalArgumentException("No such OSGiBundleState attachment: " + unit);
            this.bundle = bundle.getBundleInternal();
         }
         return bundle;
      }
   }
}
