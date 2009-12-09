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

import java.util.Set;


/**
 * A common named element
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface NamedElement
{
   /**
    * Get the bundle that owns this element
    * 
    * @return the owner
    */
   ResolverBundle getOwner();

   /**
    * Get the element name
    * 
    * @return the name
    */
   String getName();

   /**
    * Get the associated set of arbitrary attributes.
    * @return An empty list if their are no attributes.
    */
   Set<String> getAttributes();

   /**
    * Get the attribute value for the given key.
    * 
    * @param key the key
    * @return The attribute value or null.
    */
   Object getAttribute(String key);
}