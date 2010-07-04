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

import java.util.Map;

import org.jboss.classloading.plugins.metadata.ModuleCapability;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.XHostCapability;
import org.osgi.framework.Version;

/**
 * OSGiBundleCapability.
 * 
 * todo BundlePermission/PROVIDE
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 */
public class OSGiBundleCapability extends ModuleCapability implements OSGiCapability
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 2366716668262831380L;

   private AbstractBundleState bundleState;
   private XHostCapability bundleCap;

   public static OSGiBundleCapability create(XHostCapability bundleCap, AbstractBundleState bundleState)
   {
      if (bundleCap == null)
         throw new IllegalArgumentException("Null module");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");

      return new OSGiBundleCapability(bundleCap, bundleState);
   }

   private OSGiBundleCapability(XHostCapability bundleCap, AbstractBundleState bundleState)
   {
      super(bundleCap.getName(), bundleCap.getVersion());
      this.bundleState = bundleState;
      this.bundleCap = bundleCap;
      bundleCap.addAttachment(OSGiBundleCapability.class, this);
   }

   @Override
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }

   @Override
   public XHostCapability getResolverElement()
   {
      return bundleCap;
   }

   public OSGiMetaData getMetaData()
   {
      return bundleState.getOSGiMetaData();
   }

   @Override
   public boolean resolves(Module reqModule, Requirement mcreq)
   {
      if (mcreq instanceof OSGiBundleRequirement == false)
         return false;

      OSGiBundleRequirement osgireq = (OSGiBundleRequirement)mcreq;

      // Get the optional resolver
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      ResolverPlugin resolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);

      // If there is no resolver, match bundle name and version
      if (resolver == null)
      {
         boolean match = super.resolves(reqModule, mcreq);
         match &= matchAttributes(mcreq);
         return match;
      }

      // Get the wired capability from the resolver
      OSGiCapability osgicap = resolver.getWiredCapability(osgireq);
      if (osgicap != null)
      {
         boolean match = (osgicap == this);
         return match;
      }

      // A fragment can potentially attach to multiple host bundles
      // The Felix resolver has not yet settled on an API that supports that notion 
      //if (osgireq instanceof OSGiFragmentHostRequirement)
      //{
      //   boolean match = super.resolves(reqModule, mcreq);
      //   match &= matchAttributes(mcreq);
      //   return match;
      //}

      return false;
   }

   @Override
   public OSGiModule getModule()
   {
      OSGiModule module = null;
      if (bundleState instanceof DeployedBundleState)
      {
         DeployedBundleState depBundle = (DeployedBundleState)bundleState;
         DeploymentUnit unit = depBundle.getDeploymentUnit();
         module = (OSGiModule)unit.getAttachment(Module.class);
         if (module == null)
            throw new IllegalStateException("Cannot obtain module from: " + bundleState);
      }
      return module;
   }

   private boolean matchAttributes(Requirement requirement)
   {
      // Review its not clear to me from the spec whether attribute matching 
      // beyond the version should work for require-bundle?
      Version ourVersion = Version.parseVersion(getMetaData().getBundleVersion());
      OSGiBundleRequirement bundleRequirement = (OSGiBundleRequirement)requirement;
      VersionRange requiredRange = bundleRequirement.getVersionRange();
      boolean match = requiredRange.isInRange(ourVersion);

      return match;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiBundleCapability == false)
         return false;
      if (super.equals(obj) == false)
         return false;
      OSGiBundleCapability other = (OSGiBundleCapability)obj;
      return getMetaData().equals(other.getMetaData());
   }

   @Override
   protected void toString(StringBuffer buffer)
   {
      super.toString(buffer);
      ParameterizedAttribute parameters = getMetaData().getBundleParameters();
      if (parameters != null)
      {
         Map<String, Parameter> params = parameters.getAttributes();
         if (params != null && params.isEmpty() == false)
            buffer.append(" attributes=").append(params);
      }
   }
}
