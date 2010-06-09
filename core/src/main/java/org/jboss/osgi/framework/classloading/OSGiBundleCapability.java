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
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
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

   /** The bundle state */
   private AbstractBundleState bundleState;

   public static OSGiBundleCapability create(AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");

      String symbolicName = bundleState.getSymbolicName();
      Version version = bundleState.getVersion();

      return new OSGiBundleCapability(symbolicName, version, bundleState);
   }

   private OSGiBundleCapability(String name, Version version, AbstractBundleState bundleState)
   {
      super(name, version);
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      this.bundleState = bundleState;
   }

   @Override
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }

   public OSGiMetaData getMetaData()
   {
      return bundleState.getOSGiMetaData();
   }

   @Override
   public boolean resolves(Module reqModule, Requirement requirement)
   {
      if (super.resolves(reqModule, requirement) == false)
         return false;
      if (requirement instanceof OSGiBundleRequirement == false)
         return true;

      // Review its not clear to me from the spec whether attribute matching 
      // beyond the version should work for require-bundle?
      Version ourVersion = Version.parseVersion(getMetaData().getBundleVersion());
      OSGiBundleRequirement bundleRequirement = (OSGiBundleRequirement)requirement;
      VersionRange requiredRange = bundleRequirement.getVersionRange();
      if (requiredRange.isInRange(ourVersion) == false)
         return false;

      ParameterizedAttribute ourParameters = getMetaData().getBundleParameters();
      if (ourParameters == null)
         return false;

      return true;
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
