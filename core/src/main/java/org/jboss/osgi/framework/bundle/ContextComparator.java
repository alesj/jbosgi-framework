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

// $Id: $

import java.util.Comparator;

import org.jboss.dependency.spi.ControllerContext;

/**
 * If this ServiceReference and the specified ServiceReference have the same service id they are equal. 
 * This ServiceReference is less than the specified ServiceReference if it has a lower service ranking 
 * and greater if it has a higher service ranking. 
 * 
 * Otherwise, if this ServiceReference and the specified ServiceReference have the same service ranking, 
 * this ServiceReference is less than the specified ServiceReference if it has a higher service id and 
 * greater if it has a lower service id.
 *      
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 * @since 21-Mar-2010
 */
class ContextComparator implements Comparator<ControllerContext>
{
   private static final Comparator<ControllerContext> INSTANCE = new ContextComparator();

   static Comparator<ControllerContext> getInstance()
   {
      return INSTANCE;
   }

   public int compare(ControllerContext c1, ControllerContext c2)
   {
      if (c1.equals(c2))
         return 0;

      Long id1 = MDRUtils.getId(c1);
      Long id2 = MDRUtils.getId(c2);
      if (id1 != null && id1.equals(id2))
         throw new IllegalStateException("Compare not consistent with equals: " + c1 + " != " + c2);

      Integer ranking1 = MDRUtils.getRanking(c1);
      Integer ranking2 = MDRUtils.getRanking(c2);
      if (ranking1.equals(ranking2) == false)
         return (ranking1 < ranking2 ? -1 : 1);

      id1 = (id1 != null ? id1 : Long.MAX_VALUE);
      id2 = (id2 != null ? id2 : Long.MAX_VALUE);
      return (id1 > id2 ? -1 : 1);
   }
}