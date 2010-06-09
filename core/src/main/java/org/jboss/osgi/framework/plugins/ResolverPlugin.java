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
package org.jboss.osgi.framework.plugins;

import java.util.List;

import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.osgi.framework.Bundle;


//$Id$

/**
 * A plugin that handles the resolve phase of bundles.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface ResolverPlugin extends Plugin
{
   /**
    * Add a bundle to the resolver.
    * 
    * @param bundle the bundle
    * @return The resBundle associated with the added bundle.
    */
   void addBundle(Bundle bundle);
   
   /**
    * Remove a bundle from the resolver.

    * @param bundle the bundle
    * @return The resBundle associated with the removed bundle.
    */
   void removeBundle(Bundle bundle);
   
   /**
    * Resolve the given list of bundles.
    * 
    * @param bundles the bundles to resolve
    * @return The list of resolved bundles in the resolve order or an empty list
    */
   List<Bundle> resolve(List<Bundle> bundles);

   /**
    * Return true if the given capability matches the requirement.
    */
   boolean match(OSGiCapability capability, OSGiRequirement requirement);
}