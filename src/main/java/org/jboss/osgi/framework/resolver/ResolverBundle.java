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

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * An abstraction of a resBundle bundle.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface ResolverBundle
{
   /**
    * Get the underlying bundle.
    * 
    * @return the bundle
    */
   Bundle getBundle();
   
   /**
    * Get the bundle's id.
    * 
    * @return the bundle id
    */
   long getBundleId();
   
   /**
    * Get the bundle's symbolic name.
    * 
    * @return the symbolic name
    */
   String getSymbolicName();
   
   /**
    * Get the bundle's version.
    * 
    * @return the version
    */
   Version getVersion();
   
   /**
    * Get the bundle's state.
    * 
    * @return the state
    */
   int getState();
   
   /**
    * Get the list of exported packages in the declared order.
    * @return The list of exported packages or an empty list if the bundle does not export any packages.
    */
   List<ExportPackage> getExportPackages();
   
   /**
    * Get an exported package by name.
    * 
    * @param packageName the package name
    * @return The exported package or null if the bundle does not export that package.
    */
   ExportPackage getExportPackage(String packageName);

   /**
    * Get the list of imported packages in the declared order.
    * @return The list of imported packages or an empty list if the bundle does not import any packages.
    */
   List<ImportPackage> getImportPackages();
   
   /**
    * Get an imported package by name.
    * 
    * @param packageName the package name
    * @return The imported package or null if the bundle does not import that package.
    */
   ImportPackage getImportPackage(String packageName);
   
   /**
    * Get the list of required bundles.
    * 
    * @return The list of required bundles or an empty list.
    */
   List<RequiredBundle> getRequiredBundles();
   
   /**
    * Get the required bundle by symbolic name.
    * 
    * @param symbolicName the required bundle's symbolic name
    * @return null if there is no required bundle by this name
    */
   RequiredBundle getRequiredBundle(String symbolicName);
   
   /**
    * Return true if this resBundle is a singleton.
    * 
    * @return true when this is a singleton
    */
   boolean isSingleton();
   
   /**
    * Return true if this resBundle has been resolved.
    * 
    * @return true when it is resolved
    */
   boolean isResolved();

   /**
    * Mark this resBundle as resolved.
    */
   void markResolved();
}