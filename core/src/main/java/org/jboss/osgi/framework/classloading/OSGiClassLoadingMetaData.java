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

// $Id$

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.osgi.framework.metadata.NativeLibraryMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.jboss.osgi.framework.resolver.XFragmentHostRequirement;

/**
 * An extension of {@link ClassLoadingMetaData} that captures OSGi specific 
 * classloading metadata.
 * 
 * @author thomas.diesler@jboss.com
 * @since 08-Jan-2010
 */
public class OSGiClassLoadingMetaData extends ClassLoadingMetaData
{
   private static final long serialVersionUID = 1L;
   
   // The optional fragment host
   private FragmentHostMetaData fragmentHost;
   
   // The list of attached fragment classloading metadata
   private List<OSGiClassLoadingMetaData> attachedFragments = new ArrayList<OSGiClassLoadingMetaData>();
   
   // The native code libraries 
   private NativeLibraryMetaData libraries = new NativeLibraryMetaData();
   
   public FragmentHostMetaData getFragmentHost()
   {
      return fragmentHost;
   }

   public void setFragmentHost(FragmentHostMetaData fragmentHost)
   {
      this.fragmentHost = fragmentHost;
   }
   
   public List<OSGiClassLoadingMetaData> getAttachedClassLoadingMetaData()
   {
      return Collections.unmodifiableList(attachedFragments);
   }

   public void attachClassLoadingMetaData(OSGiClassLoadingMetaData fragment)
   {
      if (fragment == null)
         throw new IllegalArgumentException("Null fragment");
      if (fragment.getFragmentHost() == null)
         throw new IllegalArgumentException("Not a fragment: " + fragment);
      if (getFragmentHost() != null)
         throw new IllegalArgumentException("Cannot attach a fragment to a fragment: " + fragment);
      
      attachedFragments.add(fragment);
   }

   /**
    * Get the native libraries.
    * 
    * @return the native libraries.
    */
   public NativeLibraryMetaData getNativeLibraries()
   {
      return libraries;
   }

   /**
    * Set the native libraries.
    * 
    * @param nativeLibraries libraries the native libraries.
    * @throws IllegalArgumentException for null native libraries
    */
   public void setNativeLibraries(NativeLibraryMetaData nativeLibraries)
   {
      if (nativeLibraries == null)
         throw new IllegalArgumentException("Null libraries");
      this.libraries = nativeLibraries;
   }

   /**
    *  Fragment-Host metadata.
    */
   public static class FragmentHostMetaData
   {
      private String symbolicName;
      private AbstractVersionRange versionRange;
      private XFragmentHostRequirement fragmentHostReq;
      
      public FragmentHostMetaData(XFragmentHostRequirement fragmentHostReq)
      {
         if (fragmentHostReq == null)
            throw new IllegalArgumentException("Null fragmentHostReq");
         
         this.fragmentHostReq = fragmentHostReq;
         this.symbolicName = fragmentHostReq.getName();
         
         String versionStr = fragmentHostReq.getVersionRange().toString();
         versionRange = (AbstractVersionRange)AbstractVersionRange.valueOf(versionStr);
      }

      public XFragmentHostRequirement getResolverElement()
      {
         return fragmentHostReq;
      }

      public String getSymbolicName()
      {
         return symbolicName;
      }

      /**
       * The version range to select the the host bundle. 
       * If a range is used, then the fragment can attach to multiple hosts
       */
      public AbstractVersionRange getVersionRange()
      {
         return versionRange;
      }

      /**
       * Indicates this extension is a system or boot class path
       * extension. It is only applicable when the Fragment-Host is the System Bundle
       * 
       * The following values are supported:
       * - framework - The fragment bundle is a Framework extension bundle.
       * - bootclasspath - The fragment bundle is a boot class path extension bundle.
       */
      public String getExtension()
      {
         return fragmentHostReq.getExtension();
      }
   }
}
