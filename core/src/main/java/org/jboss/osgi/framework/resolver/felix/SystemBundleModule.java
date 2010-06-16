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
package org.jboss.osgi.framework.resolver.felix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.internal.AbstractPackageAttribute;
import org.jboss.osgi.framework.metadata.internal.AbstractParameter;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * An JBoss specific implementation of a Resolver Module
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
class SystemBundleModule extends AbstractBundleModule
{
   // Provide logging
   final Logger log = Logger.getLogger(SystemBundleModule.class);

   private List<OSGiCapability> capabilities;
   
   public SystemBundleModule(OSGiSystemState bundleState)
   {
      super(bundleState);
   }

   @Override
   List<OSGiCapability> getOSGiCapabilities()
   {
      if (capabilities == null)
      {
         capabilities = new ArrayList<OSGiCapability>();

         OSGiSystemState bundleState = OSGiSystemState.assertBundleState(getBundle());
         OSGiBundleManager bundleManager = bundleState.getBundleManager();
         SystemPackagesPlugin plugin = bundleManager.getPlugin(SystemPackagesPlugin.class);
         for (String packageSpec : plugin.getSystemPackages(true))
         {
            String packname = packageSpec;
            Version version = null;

            int versionIndex = packname.indexOf(";version=");
            if (versionIndex > 0)
            {
               packname = packageSpec.substring(0, versionIndex);
               version = Version.parseVersion(packageSpec.substring(versionIndex + 9));
            }

            Map<String, Parameter> attrs = new HashMap<String, Parameter>();
            if (version != null)
               attrs.put(Constants.VERSION_ATTRIBUTE, new AbstractParameter(version.toString()));
            
            AbstractPackageAttribute metadata = new AbstractPackageAttribute(packname, attrs, null);
            capabilities.add(OSGiPackageCapability.create(bundleState, metadata));
         }
      }
      return Collections.unmodifiableList(capabilities);
   }


   @Override
   List<OSGiRequirement> getOSGiRequirements()
   {
      return Collections.emptyList();
   }
}