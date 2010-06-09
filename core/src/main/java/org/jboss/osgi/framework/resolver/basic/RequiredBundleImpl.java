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

import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.metadata.VersionRange;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.osgi.framework.Constants;

/**
 * An abstraction of a required bundle. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class RequiredBundleImpl extends NamedElementImpl implements RequiredBundle
{
   private ResolverBundle provider;
   
   public RequiredBundleImpl(ResolverBundle owner, ParameterizedAttribute attr)
   {
      super(owner, attr);
      if (getSymbolicName().equals(owner.getSymbolicName()))
         throw new IllegalArgumentException("Cannot require a bundle with the owner's symbolic name: " + getSymbolicName());
   }

   public String getSymbolicName()
   {
      return getName();
   }

   public VersionRange getVersion()
   {
      ParameterizedAttribute attr = getParameterizedAttribute();
      String rangeStr = attr.getAttributeValue(Constants.BUNDLE_VERSION_ATTRIBUTE, String.class);
      if (rangeStr != null)
         return AbstractVersionRange.parseRangeSpec(rangeStr);
      return null;
   }

   public boolean isOptional()
   {
      boolean optional = false;
      ParameterizedAttribute attr = getParameterizedAttribute();
      Parameter param = attr.getDirective(Constants.RESOLUTION_DIRECTIVE);
      if (param != null)
         optional = Constants.RESOLUTION_OPTIONAL.equals(param.getValue());
      return optional;
   }

   public ResolverBundle getProvider()
   {
      return provider;
   }

   public void setProvider(ResolverBundle provider)
   {
      this.provider = provider;
   }

   public String toString()
   {
      return "RequiredBundle" + toShortString();
   }
}