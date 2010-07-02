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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;

/**
 * The abstract implementation of an {@link XModule}.
 *
 * This is the resolver representation of a {@link Bundle}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
class AbstractModule implements XModule
{
   private Bundle bundle;
   private XHostCapability hostCapability;
   private List<XCapability> capabilities;
   private List<XRequirement> requirements;

   AbstractModule(Bundle bundle)
   {
      this.bundle = bundle;
   }

   @Override
   public Bundle getBundle()
   {
      return bundle;
   }

   @Override
   public List<XBundleRequirement> getBundleRequirements()
   {
      List<XBundleRequirement> result = new ArrayList<XBundleRequirement>();
      for (XRequirement aux : requirements)
      {
         if (aux instanceof XBundleRequirement)
            result.add((XBundleRequirement)aux);
      }
      return Collections.unmodifiableList(result);
   }

   @Override
   public XHostCapability getHostCapability()
   {
      return hostCapability;
   }

   @Override
   public List<XPackageCapability> getPackageCapabilities()
   {
      List<XPackageCapability> result = new ArrayList<XPackageCapability>();
      for (XCapability aux : capabilities)
      {
         if (aux instanceof XPackageCapability)
         {
            XPackageCapability packcap = (XPackageCapability)aux;
            result.add(packcap);
         }
      }
      return Collections.unmodifiableList(result);
   }

   @Override
   public List<XPackageRequirement> getPackageRequirements()
   {
      List<XPackageRequirement> result = new ArrayList<XPackageRequirement>();
      for (XRequirement aux : requirements)
      {
         if (aux instanceof XPackageRequirement)
         {
            XPackageRequirement packreq = (XPackageRequirement)aux;
            if (packreq.isDynamic() == false)
               result.add(packreq);
         }
      }
      return Collections.unmodifiableList(result);
   }

   @Override
   public List<XPackageRequirement> getDynamicPackageRequirements()
   {
      List<XPackageRequirement> result = new ArrayList<XPackageRequirement>();
      for (XRequirement aux : requirements)
      {
         if (aux instanceof XPackageRequirement)
         {
            XPackageRequirement packreq = (XPackageRequirement)aux;
            if (packreq.isDynamic() == true)
               result.add(packreq);
         }
      }
      return Collections.unmodifiableList(result);
   }

   void addCapability(XCapability capability)
   {
      if (capabilities == null)
         capabilities = new ArrayList<XCapability>();

      if (capability instanceof XHostCapability)
         hostCapability = (XHostCapability)capability;

      capabilities.add(capability);
   }

   void addRequirement(XRequirement requirement)
   {
      if (requirements == null)
         requirements = new ArrayList<XRequirement>();

      requirements.add(requirement);
   }
}