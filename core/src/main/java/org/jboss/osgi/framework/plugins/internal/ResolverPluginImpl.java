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
package org.jboss.osgi.framework.plugins.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.XCapability;
import org.jboss.osgi.framework.resolver.XModule;
import org.jboss.osgi.framework.resolver.XRequirement;
import org.jboss.osgi.framework.resolver.XResolver;
import org.osgi.framework.Bundle;

/**
 * The OSGi Resolver plugin.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
public class ResolverPluginImpl extends AbstractPlugin implements ResolverPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ResolverPluginImpl.class);

   private XResolver resolver;

   public ResolverPluginImpl(OSGiBundleManager bundleManager, XResolver resolver)
   {
      super(bundleManager);
      this.resolver = resolver;
   }

   @Override
   public void addBundle(Bundle bundle)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void removeBundle(Bundle bundle)
   {
      resolver.removeModule(bundle.getBundleId());
   }

   @Override
   public List<Bundle> resolve(List<Bundle> bundles)
   {
      List<XModule> modules = new ArrayList<XModule>();
      if (bundles == null)
      {
         modules = resolver.getModules();
      }
      else
      {
         for (Bundle bundle : bundles)
         {
            XModule module = resolver.findModuleById(bundle.getBundleId());
            if (module == null)
               throw new IllegalStateException("Module not registered for: " + bundle);
            
            modules.add(module);
         }
      }
      
      List<Bundle> result = new ArrayList<Bundle>();
      modules = resolver.resolve(modules);
      for (XModule module : modules)
      {
         Bundle bundle = module.getAttachment(Bundle.class);
         if (bundle == null)
            throw new IllegalStateException("Cannot obtain associated bundle from: " + module);
         
         result.add(bundle);
      }
      
      return Collections.unmodifiableList(result);
   }

   @Override
   public OSGiCapability getWiredCapability(OSGiRequirement osgireq)
   {
      XRequirement req = osgireq.getResolverElement();
      XCapability cap = req.getWiredCapability();
      if (cap == null)
         return null;
      
      return cap.getAttachment(OSGiCapability.class);
   }

}