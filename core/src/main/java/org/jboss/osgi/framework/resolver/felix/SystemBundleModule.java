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
import java.util.List;

import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.manifestparser.CapabilityImpl;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.osgi.framework.resolver.AbstractModule;
import org.osgi.framework.Version;

/**
 * An JBoss specific implementation of a Resolver Module
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
class SystemBundleModule extends AbstractModule
{
   // Provide logging
   final Logger log = Logger.getLogger(SystemBundleModule.class);

   public SystemBundleModule(OSGiSystemState bundleState)
   {
      super(bundleState);
   }

   @Override
   public List<Capability> createCapabilities()
   {
      List<Capability> capList = new ArrayList<Capability>();

      AbstractBundleState bundleState = AbstractBundleState.assertBundleState(getBundle());
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

         // Get the capabiliy attributes
         List<Attribute> attrs = new ArrayList<Attribute>();
         attrs.add(new Attribute(Capability.PACKAGE_ATTR, packname, false));
         if (version != null)
            attrs.add(new Attribute(Capability.VERSION_ATTR, version, false));

         // Get the capabiliy directives
         List<Directive> dirs = new ArrayList<Directive>();

         capList.add(new CapabilityImpl(this, Capability.PACKAGE_NAMESPACE, dirs, attrs));
      }

      return Collections.unmodifiableList(capList);
   }

   @Override
   public List<Requirement> createRequirements()
   {
      return Collections.emptyList();
   }

   @Override
   protected List<Requirement> createDynamicRequirements()
   {
      return Collections.emptyList();
   }
}