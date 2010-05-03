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

// $Id: $

import org.jboss.classloader.spi.filter.PackageClassFilter;

/**
 * A class filter for OSGi Core packages.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Feb-2010
 */
public final class OSGiCoreClassFilter extends PackageClassFilter
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 5679852972501066041L;

   /** The singleton instance */ 
   public static OSGiCoreClassFilter INSTANCE = new OSGiCoreClassFilter();
   
   private OSGiCoreClassFilter()
   {
      super(getCorePackages());
   }
   
   public static String[] getCorePackages()
   {
      return new String[] {
         "org.osgi.framework",
         "org.osgi.framework.hooks",
         "org.osgi.framework.hooks.service",
         "org.osgi.framework.launch",
         "org.osgi.service.condpermadmin",
         "org.osgi.service.packageadmin",
         "org.osgi.service.permissionadmin",
         "org.osgi.service.startlevel",
         "org.osgi.service.url",
         "org.osgi.util.tracker"
      };
   }

   public String toString()
   {
      return "OSGI_CORE";
   }

   @Override
   public void setIncludeJava(boolean includeJava)
   {
      throw new UnsupportedOperationException("Cannot modify OSGi Core Filter");
   }
}