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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The abstract implementation of a {@link XCapability}.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
class AbstractElement implements XNamedElement
{
   private XModule module;
   private String name;
   private Map<String, String> directives;
   private Map<String, String> attributes;

   public AbstractElement(AbstractModule module, String name, Map<String, String> dirs, Map<String, String> atts)
   {
      this.module = module;
      this.name = name;

      if (dirs != null)
         directives = new HashMap<String, String>(dirs);
      if (atts != null)
         attributes = new HashMap<String, String>(atts);
   }

   @Override
   public XModule getModule()
   {
      return module;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getAttribute(String key)
   {
      return attributes != null ? attributes.get(key) : null;
   }

   @Override
   public String getDirective(String key)
   {
      return directives != null ? directives.get(key) : null;
   }

   @Override
   public Map<String, String> getAttributes()
   {
      return Collections.unmodifiableMap(attributes);
   }

   @Override
   public Map<String, String> getDirectives()
   {
      return Collections.unmodifiableMap(directives);
   }
}