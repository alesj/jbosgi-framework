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
import org.apache.felix.framework.resolver.Wire;
import org.jboss.osgi.framework.resolver.AbstractModule;
import org.jboss.osgi.framework.resolver.AbstractPackageRequirement;
import org.jboss.osgi.framework.resolver.AbstractWire;
import org.jboss.osgi.framework.resolver.XCapability;
import org.jboss.osgi.framework.resolver.XModule;
import org.jboss.osgi.framework.resolver.XPackageCapability;
import org.jboss.osgi.framework.resolver.XPackageRequirement;
import org.jboss.osgi.framework.resolver.XRequirement;
import org.jboss.osgi.framework.resolver.XWire;

/**
 * A processor for the resolver results.
 * 
 * The resolver API esablishes rules about the wiring results.
 * 
 * These are
 *
 *  <ul>
 *    <li>A mandatory {@link XRequirement} in a resoved {@link XModule} must have a {@link XWire}</li>
 *  </ul>
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

   public void setModuleWires(AbstractModule module, List<Wire> fwires)
   {
      List<XWire> result = new ArrayList<XWire>();

      // Iterate over all standard requirements
      for (XRequirement req : module.getRequirements())
      {
         XModule importer = req.getModule();

         // Get the associated felix requirement
         Requirement freq = req.getAttachment(Requirement.class);
         if (freq == null)
            throw new IllegalStateException("Cannot obtain felix requirement from: " + req);

         // Find the wire that corresponds to the felix requirement
         Wire fwire = getWireForRequirement(fwires, freq);
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
         XCapability cap = null;
         for (XCapability aux : exporter.getCapabilities())
         {
            if (aux.getAttachment(Capability.class) == fcap)
            {
               cap = aux;
               break;
            }
         }

         AbstractWire wire = new AbstractWire(importer, req, exporter, cap);
         result.add(wire);
      }

      resolver.setWires(module, result);
   }

   private void handleNullWire(List<XWire> result, XRequirement req)
   {
      XWire wire = null;

      // Felix does not maintain wires to capabilies provided by the same bundle
      if (req instanceof XPackageRequirement)
      {
         XModule module = req.getModule();
         for (XPackageCapability cap : module.getPackageCapabilities())
         {
            // Add a wire if there is a match to a capability provided by the same module 
            if (((AbstractPackageRequirement)req).match(cap))
            {
               wire = new AbstractWire(module, req, module, cap);
               break;
            }
         }
      }

      if (wire == null && req.isOptional() == false)
         throw new IllegalStateException("Cannot find a wire for mandatory requirement: " + req);

      result.add(wire);
   }

   public void setResolved(AbstractModule module)
   {
      resolver.setResolved(module);
   }

   private Wire getWireForRequirement(List<Wire> fwires, Requirement freq)
   {
      for (Wire fwire : fwires)
      {
         if (fwire.getRequirement() == freq)
            return fwire;
      }
      return null;
   }
}