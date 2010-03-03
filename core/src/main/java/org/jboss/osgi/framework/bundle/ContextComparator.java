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

import java.util.Comparator;

import org.jboss.dependency.spi.ControllerContext;

/**
 * Compare controller contexts.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class ContextComparator implements Comparator<ControllerContext>
{
   public static final Comparator<ControllerContext> INSTANCE = new ContextComparator();
   
   public int compare(ControllerContext c1, ControllerContext c2)
   {
      Integer ranking1 = MDRUtils.getRanking(c1);
      Integer ranking2 = MDRUtils.getRanking(c2);
      int diff = ranking2 - ranking1;
      if (diff == 0)
      {
         Long id1 = MDRUtils.getId(c1);
         Long id2 = MDRUtils.getId(c2);
         if (id1 == null && id2 == null)
            return 0;
         if (id1 != null && id2 == null)
            return -1;
         if (id2 != null && id1 == null)
            return 1;

         return (id2 > id1) ? -1 : 1;
      }
      return diff;
   }
}