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
package org.jboss.osgi.framework.classloading;

// $Id: OSGiClassLoaderFactory.java 95177 2009-10-20 15:14:31Z thomas.diesler@jboss.com $

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.osgi.framework.deployers.OSGiBundleNativeCodeDeployer;

/**
 * An OSGi bundle class loader.
 * 
 * This implementation supports the notion of OSGi Native Code Libraries.
 * The library map is initialized in {@link OSGiBundleNativeCodeDeployer}.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 19-Dec-2209
 */
public class OSGiBundleClassLoader extends BaseClassLoader
{
   private OSGiClassLoaderPolicy osgiPolicy;

   public OSGiBundleClassLoader(ClassLoaderPolicy policy)
   {
      super(policy);

      if (policy instanceof OSGiClassLoaderPolicy)
         osgiPolicy = (OSGiClassLoaderPolicy)policy;
   }

   @Override
   protected String findLibrary(String libname)
   {
      String libraryPath = null;

      if (osgiPolicy != null)
         libraryPath = osgiPolicy.findLibrary(libname);

      if (libraryPath == null)
         libraryPath = super.findLibrary(libname);

      return libraryPath;
   }

}
