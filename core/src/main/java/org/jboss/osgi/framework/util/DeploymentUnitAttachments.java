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
package org.jboss.osgi.framework.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.spi.Attachments;

/**
 * An implementation of {@link Attachments} that delegates to a 
 * {@link DeploymentUnit}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 26-Oct-2009
 */
public class DeploymentUnitAttachments implements Attachments
{
   private DeploymentUnit delegate;
   
   public DeploymentUnitAttachments(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null unit");
      this.delegate = unit;
   }

   public <T> T addAttachment(Class<T> clazz, T value)
   {
      return delegate.addAttachment(clazz, value);
   }

   public <T> T addAttachment(String name, T value, Class<T> clazz)
   {
      return delegate.addAttachment(name, value, clazz);
   }

   public Object addAttachment(String name, Object value)
   {
      return delegate.addAttachment(name, value);
   }

   public <T> T getAttachment(String name, Class<T> clazz)
   {
      return delegate.getAttachment(name, clazz);
   }

   public <T> T getAttachment(Class<T> clazz)
   {
      return delegate.getAttachment(clazz);
   }

   public Object getAttachment(String name)
   {
      return delegate.getAttachment(name);
   }

   public Collection<Key> getAttachmentKeys()
   {
      List<Key> keys = new ArrayList<Key>();
      Set<String> keySet = delegate.getAttachments().keySet();
      for(String key : keySet)
      {
         keys.add(new Key(key, null));
      }
      return Collections.unmodifiableList(keys);
   }

   public <T> T removeAttachment(Class<T> clazz, String name)
   {
      return delegate.removeAttachment(name, clazz);
   }

   public <T> T removeAttachment(Class<T> clazz)
   {
      return delegate.removeAttachment(clazz);
   }

   public Object removeAttachment(String name)
   {
      return delegate.removeAttachment(name);
   } 

}
