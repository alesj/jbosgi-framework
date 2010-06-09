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
package org.jboss.osgi.framework.resolver.basic;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Handles the resolve phase of the installed bundles.
 *  
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface Resolver
{
   /**
    * Get the registered resBundle bundles. 
    * @return The list of registered resBundle bundles.
    */
   List<ResolverBundle> getBundles();
   
   /**
    * Get the registered resBundle bundle.
    * 
    * @param symbolicName the symbolic name of the bundle
    * @param version the version of the bundle
    * @return The registered resBundle bundle.
    */
   ResolverBundle getBundle(String symbolicName, Version version);
   
   /**
    * Get the registered resBundle bundle.
    * 
    * @param bundle the bundle 
    * @return The registered resBundle bundle.
    */
   ResolverBundle getBundle(Bundle bundle);
   
   /**
    * Add a bundle to the resolver.
    * 
    * @param bundle the bundle
    * @return The resBundle associated with the added bundle.
    */
   ResolverBundle addBundle(Bundle bundle);
   
   /**
    * Remove a bundle from the resolver.

    * @param bundle the bundle
    * @return The resBundle associated with the removed bundle.
    */
   ResolverBundle removeBundle(Bundle bundle);
   
   /**
    * Resolve the given list of bundles.
    * 
    * @param bundles the bundles to resolve
    * @return The list of resolved bundles in the resolve order or an empty list
    */
   List<ResolverBundle> resolve(List<Bundle> bundles);

   /**
    * Get the exporter for the given import package.
    * 
    * @param importer The bundle that imports the package.
    * @param packageName The import package name
    * @return The export package that the import is wired to or null if the import is not yet resolved.
    */
   ExportPackage getExporter(Bundle importer, String packageName);
   
   /**
    * Return true if the given importer is wired to the given exporter for the given package name.
    * 
    * @param importer The bundle that imports the package.
    * @param exporter The bundle that exports the package.
    * @param packageName The import package name
    * @return True if the importer is wired to the exporter
    */
   boolean match(Bundle importer, Bundle exporter, String packageName);
}