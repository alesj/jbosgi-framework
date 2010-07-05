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
package org.jboss.osgi.resolver.felix;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.FragmentRequirement;
import org.apache.felix.framework.resolver.Wire;
import org.jboss.osgi.resolver.XBundleCapability;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XFragmentHostRequirement;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.resolver.XPackageCapability;
import org.jboss.osgi.resolver.XPackageRequirement;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XWire;
import org.jboss.osgi.resolver.spi.AbstractModule;
import org.jboss.osgi.resolver.spi.AbstractPackageRequirement;

/**
 * A processor for the resolver results.
 * 
 * A mandatory {@link XRequirement} in a resoved {@link XModule} must have a {@link XWire}</li>
 *  
 * @author thomas.diesler@jboss.com
 * @since 05-Jul-2010
 */
public class ResultProcessor
{
   private FelixResolver resolver;

   ResultProcessor(FelixResolver resolver)
   {
      this.resolver = resolver;
   }

   public void setModuleWires(ModuleExt moduleExt, List<Wire> fwires)
   {
      // Set the wires on the felix module
      moduleExt.setWires(fwires);

      // Iterate over all standard requirements
      List<XWire> result = new ArrayList<XWire>();
      AbstractModule module = moduleExt.getModule();
      for (XRequirement req : module.getRequirements())
      {
         AbstractModule importer = (AbstractModule)req.getModule();

         // Get the associated felix requirement
         Requirement freq = req.getAttachment(Requirement.class);
         if (freq == null)
            throw new IllegalStateException("Cannot obtain felix requirement from: " + req);

         // Find the wire that corresponds to the felix requirement
         Wire fwire = findWireForRequirement(fwires, freq);
         if (fwire == null)
         {
            handleNullWire(result, req);
            continue;
         }
         
         // Get the exporter
         Capability fcap = fwire.getCapability();
         ModuleExt fexporter = (ModuleExt)fwire.getExporter();
         XModule exporter = fexporter.getModule();

         // Find the coresponding capability
         XCapability cap = findCapability(exporter, fcap);
         resolver.addWire(importer, req, exporter, cap);
      }
   }

   private void handleNullWire(List<XWire> result, XRequirement req)
   {
      XWire wire = null;
      
      // Felix does not maintain wires to capabilies provided by the same bundle
      if (req instanceof XPackageRequirement)
      {
         AbstractModule importer = (AbstractModule)req.getModule();
         XPackageCapability cap = getMatchingPackageCapability(importer, req);
         
         // If the importer is a fragment, scan the host's wires
         if (cap == null && importer.isFragment())
         {
            Requirement freq = req.getAttachment(Requirement.class);
            
            XModule host = getFragmentHost(importer);
            ModuleExt hostExt = host.getAttachment(ModuleExt.class);
            Wire fwire = findWireForRequirement(hostExt.getWires(), freq);
            if (fwire != null)
            {
               XModule exporter = ((ModuleExt)fwire.getExporter()).getModule();
               cap = (XPackageCapability)findCapability(exporter, fwire.getCapability());
            }
         }
         
         // Add the additional wire
         if (cap != null)
         {
            XModule exporter = cap.getModule();
            wire = resolver.addWire(importer, req, exporter, cap);
         }
      }
      
      // Provide a wire to the Fragment-Host bundle capability 
      else if (req instanceof XFragmentHostRequirement)
      {
         AbstractModule fragModule = (AbstractModule)req.getModule();
         XModule hostModule = getFragmentHost(fragModule);
         XBundleCapability hostCap = hostModule.getBundleCapability();
         wire = resolver.addWire(fragModule, req, hostModule, hostCap);
      }
      
      if (wire == null && req.isOptional() == false)
         throw new IllegalStateException("Cannot find a wire for mandatory requirement: " + req);
   }

   private XPackageCapability getMatchingPackageCapability(XModule module, XRequirement req)
   {
      for (XPackageCapability cap : module.getPackageCapabilities())
      {
         // Add a wire if there is a match to a capability provided by the same module 
         if (((AbstractPackageRequirement)req).match(cap))
            return cap;
      }
      return null;
   }

   private XModule getFragmentHost(XModule fragModule)
   {
      ModuleExt ffrag = fragModule.getAttachment(ModuleExt.class);
      ModuleExt fHost = resolver.findHost(ffrag);
      XModule hostModule = fHost.getModule();
      return hostModule;
   }

   public void setResolved(ModuleExt moduleExt)
   {
      moduleExt.setResolved();
      resolver.setResolved(moduleExt.getModule());
   }

   private Wire findWireForRequirement(List<Wire> fwires, Requirement freq)
   {
      Wire fwire = null;
      if (fwires != null)
      {
         for (Wire aux : fwires)
         {
            Requirement auxreq = aux.getRequirement();
            if (auxreq == freq)
            {
               fwire = aux;
               break;
            }
            
            if (auxreq instanceof FragmentRequirement)
            {
               auxreq = ((FragmentRequirement)auxreq).getRequirement();
               if (auxreq == freq)
               {
                  fwire = aux;
                  break;
               }
            }
         }
      }
      return fwire;
   }

   private XCapability findCapability(XModule exporter, Capability fcap)
   {
      for (XCapability aux : exporter.getCapabilities())
      {
         if (aux.getAttachment(Capability.class) == fcap)
            return aux;
      }
      return null;
   }
}