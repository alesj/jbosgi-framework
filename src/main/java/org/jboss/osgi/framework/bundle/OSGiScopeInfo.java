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

import org.jboss.dependency.plugins.AbstractScopeInfo;
import org.jboss.dependency.spi.ScopeInfo;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;

/**
 * OSGi ScopeInfo
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class OSGiScopeInfo extends AbstractScopeInfo
{
   private OSGiServiceState serviceState;

   private OSGiScopeInfo(Object name, String className, OSGiServiceState serviceState)
   {
      super(name, className);
      this.serviceState = serviceState;
   }

   /**
    * Create scope info.
    *
    * @param name the name
    * @param className the class name
    * @param clazz the class
    * @param serviceState the service state
    * @return new scope info
    */
   static ScopeInfo createScopeInfo(Object name, String className, Class<?> clazz, OSGiServiceState serviceState)
   {
      if (className == null && clazz != null)
         className = clazz.getName();

      OSGiScopeInfo result = new OSGiScopeInfo(name, className, serviceState);
      if (clazz != null)
      {
         ScopeKey key = result.getScope();
         key.addScope(new Scope(CommonLevels.CLASS, clazz));
      }
      return result;
   }

   @Override
   public ScopeKey getScope()
   {
      // THIS IS A HACK - the scope originally gets initialise with a class name, we fix it to have the class
      ScopeKey key = super.getScope();
      Scope scope = key.getScope(CommonLevels.CLASS);
      if (scope == null)
         return key;
      Object qualifier = scope.getQualifier();
      if (qualifier instanceof Class)
         return key;

      Object service = serviceState.getTarget();
      if (service != null)
      {
         Class<?> clazz = service.getClass();
         key.addScope(new Scope(CommonLevels.CLASS, clazz));
      }
      return key;
   }
}