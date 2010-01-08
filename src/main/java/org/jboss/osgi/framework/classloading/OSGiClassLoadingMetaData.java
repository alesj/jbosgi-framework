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
import org.osgi.framework.Version;

/**
 * An extension of {@link ClassLoadingMetaData} that captures OSGi specific 
 * classloading metadata.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 08-Jan-2010
 */
public class OSGiClassLoadingMetaData extends ClassLoadingMetaData
{
   private static final long serialVersionUID = 1L;
   
   // The optional fragment host
   private FragmentHost fragmentHost;
   // The list of attached fragment classloading metadata
   private List<OSGiClassLoadingMetaData> attachedFragments = new ArrayList<OSGiClassLoadingMetaData>();
   
   public FragmentHost getFragmentHost()
   {
      return fragmentHost;
   }

   public void setFragmentHost(FragmentHost fragmentHost)
   {
      this.fragmentHost = fragmentHost;
   }
   
   public List<OSGiClassLoadingMetaData> getAttachedFragmentMetaData()
   {
      return Collections.unmodifiableList(attachedFragments);
   }

   public void attachedFragmentMetaData(OSGiClassLoadingMetaData fragment)
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
    *  Fragment-Host metadata.
    */
   public static class FragmentHost
   {
      private String symbolicName;
      private Version bundleVersion;
      private String extension;
      
      public FragmentHost(String symbolicName)
      {
         if (symbolicName == null)
            throw new IllegalArgumentException("Null symbolicName");
         
         this.symbolicName = symbolicName;
      }

      public String getSymbolicName()
      {
         return symbolicName;
      }

      public Version getBundleVersion()
      {
         return bundleVersion;
      }

      public String getExtension()
      {
         return extension;
      }

      public void setExtension(String extension)
      {
         this.extension = extension;
      }

      public void setBundleVersion(Version bundleVersion)
      {
         this.bundleVersion = bundleVersion;
      }
   }
}
