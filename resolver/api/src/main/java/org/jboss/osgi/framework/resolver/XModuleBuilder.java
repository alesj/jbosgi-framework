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
package org.jboss.osgi.framework.resolver;

import java.util.Map;

import org.osgi.framework.Version;

/**
 * A builder for resolver modules
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
public final class XModuleBuilder
{
   private AbstractModule module;

   private XModuleBuilder()
   {
   }

   public XModule getModule()
   {
      return module;
   }

   /**
    * Get a new module builder
    */
   public static XModuleBuilder newBuilder()
   {
      return new XModuleBuilder();
   }

   /**
    * Get a new module and associate it with this builder
    * @param symbolicName The module symbolic name
    * @param version The module version
    */
   public XModule createModule(long moduleId, String symbolicName, Version version)
   {
      module = new AbstractModule(moduleId, symbolicName, version);
      return module;
   }

   /**
    * Add a bundle capability
    * @param symbolicName The bundle symbolic name
    * @param version The bundle version
    */
   public XBundleCapability addBundleCapability(String symbolicName, Version version)
   {
      XBundleCapability cap = new AbstractBundleCapability(module, symbolicName, version);
      module.addCapability(cap);
      return cap;
   }

   /**
    * Add a bundle requirement
    * @param symbolicName The bundle symbolic name
    * @param dirs The directives
    * @param atts The attributes
    */
   public XRequireBundleRequirement addBundleRequirement(String symbolicName, Map<String, String> dirs, Map<String, String> atts)
   {
      XRequireBundleRequirement req = new AbstractBundleRequirement(module, symbolicName, dirs, atts);
      module.addRequirement(req);
      return req;
   }

   /**
    * Add a package capability
    * @param name The package name
    * @param dirs The directives
    * @param atts The attributes
    */
   public XPackageCapability addPackageCapability(String name, Map<String, String> dirs, Map<String, String> atts)
   {
      XPackageCapability cap = new AbstractPackageCapability(module, name, dirs, atts);
      module.addCapability(cap);
      return cap;
   }

   /**
    * Add a package capability
    * @param name The package name
    * @param dirs The directives
    * @param atts The attributes
    */
   public XPackageRequirement addPackageRequirement(String name, Map<String, String> dirs, Map<String, String> atts)
   {
      XPackageRequirement req = new AbstractPackageRequirement(module, name, dirs, atts, false);
      module.addRequirement(req);
      return req;
   }

   /**
    * Add a package capability
    * @param name The package name
    * @param atts The attributes
    */
   public XPackageRequirement addDynamicPackageRequirement(String name, Map<String, String> atts)
   {
      XPackageRequirement req = new AbstractPackageRequirement(module, name, null, atts, true);
      module.addRequirement(req);
      return req;
   }
}