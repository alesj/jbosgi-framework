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
package org.jboss.osgi.framework.bundle;

import java.util.Dictionary;
import java.util.Set;

import org.jboss.beans.metadata.api.model.QualifierContent;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.spi.qualifier.QualifierMatcher;
import org.jboss.kernel.spi.qualifier.QualifierParser;
import org.jboss.metadata.spi.MetaData;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * OSGi filter parsing and matching.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class FilterParserAndMatcher implements QualifierParser, QualifierMatcher<Filter>
{
   static final FilterParserAndMatcher INSTANCE = new FilterParserAndMatcher();

   private FilterParserAndMatcher()
   {
   }

   public Class<Filter> getHandledType()
   {
      return Filter.class;
   }

   public QualifierContent getHandledContent()
   {
      return QualifierContent.getContent("filter");
   }

   public boolean matches(ControllerContext context, Set<Object> suppliedQualifiers, Filter filter)
   {
      MetaData metaData = context.getScopeInfo().getMetaData();
      if (metaData == null)
         return false;

      MetaData instanceMD = metaData.getScopeMetaData(CommonLevels.INSTANCE);
      if (instanceMD == null)
         return false;
      
      Dictionary dictionary = instanceMD.getMetaData(Dictionary.class);
      return dictionary != null && filter.match(dictionary);
   }

   public Object parseWanted(ClassLoader cl, Object rawQualifier)
   {
      try
      {
         return FrameworkUtil.createFilter(String.valueOf(rawQualifier));
      }
      catch (InvalidSyntaxException e)
      {
         throw new IllegalArgumentException(e);
      }
   }

   public Object parseSupplied(ClassLoader cl, Object rawQualifier)
   {
      return parseWanted(cl, rawQualifier);
   }
}