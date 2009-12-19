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
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * OSGiBundleCapability.
 * 
 * todo BundlePermission/PROVIDE
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleCapability extends ModuleCapability
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 2366716668262831380L;

   /** The bundle state */
   private OSGiBundleState bundleState;

   /**
    * Create a new OSGiBundleCapability
    * 
    * @param bundleState the bundleState
    * @return the capability
    * @throws IllegalArgumentException for a null metadata
    */
   public static OSGiBundleCapability create(OSGiBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");

      String symbolicName = bundleState.getSymbolicName();
      Version version = bundleState.getVersion();

      return new OSGiBundleCapability(symbolicName, version, bundleState);
   }
   
   /**
    * Create a new OSGiBundleCapability.
    * 
    * @param name the name
    * @param version the version pass null of the default version
    * @param metadata the metadata
    * @throws IllegalArgumentException for a null name or requireBundle
    */
   public OSGiBundleCapability(String name, Version version, OSGiBundleState bundleState)
   {
      super(name, version);
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      this.bundleState = bundleState;
   }
   
   /**
    * Get the metadata.
    * 
    * @return the metadata.
    */
   public OSGiMetaData getMetaData()
   {
      return bundleState.getMetaData();
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
      OSGiBundleRequirement bundleRequirement = (OSGiBundleRequirement) requirement;
      OSGiMetaData metaData = getMetaData();
      ParameterizedAttribute ourParameters = metaData.getBundleParameters();
      ParameterizedAttribute otherParameters = bundleRequirement.getRequireBundle();
      if (otherParameters != null)
      {
         Map<String, Parameter> params = otherParameters.getAttributes();
         if (params != null && params.isEmpty() == false)
         {
            for (String name : params.keySet())
            {
               if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name) == false)
               {
                  if (ourParameters == null)
                     return false;
                  String ourValue = ourParameters.getAttributeValue(name, String.class);
                  if (ourValue == null)
                     return false;
                  if (ourValue.equals(otherParameters.getAttributeValue(name, String.class)) == false)
                     return false;
               }
            }
         }
      }
      return true;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiBundleCapability == false)
         return false;
      if (super.equals(obj) ==false)
         return false;
      OSGiBundleCapability other = (OSGiBundleCapability) obj;
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
