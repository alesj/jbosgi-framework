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

import java.util.Map;
import java.util.Set;

import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;

/**
 * A common named element
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
abstract class NamedElementImpl implements NamedElement
{
   private ResolverBundle owner;
   private ParameterizedAttribute paramattr;

   public NamedElementImpl(ResolverBundle owner, ParameterizedAttribute attr)
   {
      if (owner == null)
         throw new IllegalArgumentException("Null element owner");
      if (attr == null)
         throw new IllegalArgumentException("Null element attribute");

      this.owner = owner;
      this.paramattr = attr;
   }

   public ResolverBundle getOwner()
   {
      return owner;
   }

   public String getName()
   {
      return paramattr.getAttribute();
   }

   protected ParameterizedAttribute getParameterizedAttribute()
   {
      return paramattr;
   }

   public Set<String> getAttributes()
   {
      return getParameterizedAttribute().getAttributes().keySet();
   }

   public Object getAttribute(String key)
   {
      Parameter attr = getParameterizedAttribute().getAttribute(key);
      return (attr != null ? attr.getValue() : null);
   }

   private String shortString;
   public String toShortString()
   {
      if (shortString == null)
      {
         Map<String, Parameter> attributes = getParameterizedAttribute().getAttributes();
         Map<String, Parameter> directives = getParameterizedAttribute().getDirectives();
         StringBuffer buffer = new StringBuffer("[" + getName());
         for (Map.Entry<String, Parameter> entry : directives.entrySet())
            buffer.append(";" + entry.getKey() + ":=" + entry.getValue().getValue());
         for (Map.Entry<String, Parameter> entry : attributes.entrySet())
            buffer.append(";" + entry.getKey() + "=" + entry.getValue().getValue());
         buffer.append("]");
         shortString = buffer.toString();
      }
      return shortString;
   }
}