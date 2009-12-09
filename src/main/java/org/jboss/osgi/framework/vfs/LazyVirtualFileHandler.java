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
package org.jboss.osgi.framework.vfs;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Create a lazy VirtualFileHandler based off VirtualFile
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class LazyVirtualFileHandler
{
   private static final Method getHandler;

   static
   {
      try
      {
         PrivilegedExceptionAction<Method> action = new PrivilegedExceptionAction<Method>()
         {
            public Method run() throws Exception
            {
               Method method = VirtualFile.class.getDeclaredMethod("getHandler");
               method.setAccessible(true);
               return method;
            }
         };
         getHandler = AccessController.doPrivileged(action);

      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Create a VFH proxy, delaying the actual reflect hack.
    *
    * @param file the VFH owner
    * @return VFH proxy
    * @throws IOException for any error
    */
   static VirtualFileHandler create(VirtualFile file) throws IOException
   {
      if (file == null)
         return null;

      ClassLoader cl = LazyVirtualFileHandler.class.getClassLoader();
      Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{VirtualFileHandler.class}, new ProxyHandler(file));
      return VirtualFileHandler.class.cast(proxy);
   }
   
   private static class ProxyHandler implements InvocationHandler
   {
      private VirtualFile file;
      private volatile VirtualFileHandler handler;

      private ProxyHandler(VirtualFile file)
      {
         this.file = file;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         return method.invoke(getHandler(), args);
      }

      private VirtualFileHandler getHandler() throws Exception
      {
         if (handler == null)
            handler = (VirtualFileHandler)getHandler.invoke(file);

         return handler;
      }
   }
}