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

import org.jboss.osgi.framework.metadata.VersionRange;

/**
 * An abstraction of a required bundle.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface RequiredBundle extends NamedElement
{
   /**
    * Get the bundle's symbolic name.
    * 
    * @return the symbolic name
    */
   String getSymbolicName();
   
   /**
    * The version range of the required bundle.
    * 
    * @return null if this attribute is not set
    */
   VersionRange getVersion();
   
   /**
    * True if the resolution directive for the given required bundle is 'optional' 
    * 
    * @return null if this attribute is not set
    */
   boolean isOptional();
   
   /**
    * Get the provider for this bundle requirement
    * 
    * @return the owner
    */
   ResolverBundle getProvider();

   /**
    * Set the provider for this bundle requirement
    */
   void setProvider(ResolverBundle provider);
}