/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.resolver.internal;

import java.util.ArrayList;
import java.util.List;

import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.ManifestParser;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.osgi.framework.Bundle;

/**
 * A ResolverBundle implementation for the system bundle.
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Nov-2009
 */
public class ResolverSystemBundleImpl extends AbstractResolverBundle
{
   public ResolverSystemBundleImpl(Bundle bundle)
   {
      super(bundle);

      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      SystemPackagesPlugin plugin = bundleManager.getPlugin(SystemPackagesPlugin.class);

      // Get the list of system packages
      List<PackageAttribute> packageList = new ArrayList<PackageAttribute>();
      String sysPackages = plugin.getSystemPackages(true).toString();
      sysPackages = sysPackages.substring(1, sysPackages.length() - 1);
      ManifestParser.parsePackages(sysPackages, packageList);
      
      // Initialize exported packages
      for (PackageAttribute attr : packageList)
      {
         String packageName = attr.getAttribute();
         exportedPackages.put(packageName, new ExportPackageImpl(this, attr));
      }
   }

   public boolean isSingleton()
   {
      return true;
   }
}